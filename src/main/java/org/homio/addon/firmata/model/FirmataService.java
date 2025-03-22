package org.homio.addon.firmata.model;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.firmata4j.Consumer;
import org.firmata4j.I2CDevice;
import org.firmata4j.IODevice;
import org.firmata4j.IODeviceEventListener;
import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.fsm.Event;
import org.firmata4j.transport.NetworkTransport;
import org.homio.addon.firmata.provider.FirmataCommandPlugins;
import org.homio.addon.firmata.provider.IODeviceWrapper;
import org.homio.addon.firmata.provider.command.FirmataCommandPlugin;
import org.homio.addon.firmata.provider.command.FirmataOneWireResponseDataCommand;
import org.homio.addon.firmata.provider.util.HomioFirmataUtil;
import org.homio.addon.firmata.provider.util.OneWireDevice;
import org.homio.api.Context;
import org.homio.api.ContextNetwork;
import org.homio.api.model.Icon;
import org.homio.api.model.Status;
import org.homio.api.service.EntityService;
import org.homio.api.state.DecimalType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.firmata4j.firmata.parser.FirmataEventType.SYSEX_CUSTOM_MESSAGE;
import static org.firmata4j.firmata.parser.FirmataToken.END_SYSEX;
import static org.firmata4j.firmata.parser.FirmataToken.START_SYSEX;

public class FirmataService extends EntityService.ServiceInstance<FirmataBaseEntity<?>>
  implements IODeviceEventListener {

  public static final byte SYSEX_INIT = 0x40;
  public static final byte SYSEX_PING = 0x41;

  @Getter
  private final FirmataOneWireResponseDataCommand oneWireCommand;

  private final FirmataCommandPlugins firmataCommandPlugins;
  @Getter
  private final Map<String, FirmataPinEndpoint> endpoints = new HashMap<>();
  private IODeviceWrapper device;
  private IODevice ioDevice;
  private Long lastRestartAttempt = 0L;
  @Setter
  private Consumer<DeviceInfo> afterStartHandler;
  private long deviceStartTime;
  private long lastPingResponseTime;
  private long lastInitResponseTime;
  private long lastPingRequestTime;

  public FirmataService(@NotNull Context context, @NotNull FirmataBaseEntity entity, boolean fireInitialize) {
    super(context, entity, fireInitialize, "Firmata");
    this.oneWireCommand = context.getBean(FirmataOneWireResponseDataCommand.class);
    this.firmataCommandPlugins = context.getBean(FirmataCommandPlugins.class);

    // fires when service connected to a device
    this.afterStartHandler = new Consumer<>() {
      @Override
      public void accept(DeviceInfo info) {
        deviceStartTime = System.currentTimeMillis() - info.started * 1000;
        if (entity.tryUpdateEntity(info)) {
          context.db().save(entity);
        }
        // create a top group if required
        String topGroupID = context().var().createGroup("firmata", "Firmata", builder ->
          builder.setIcon(new Icon("fas fa-microchip", "#27966E")));

        var groupName = "Firmata " + entity.getBoard() + "(" + info.name + ")";
        context().var().createSubGroup(topGroupID, info.deviceID, groupName, builder ->
          builder.setIcon(new Icon("fas fa-hard-drive", "#28A6A2")));

        entity.setStatusOnline();
        log.info("[{}]: Firmata device <{}> joined successfully", entity.getEntityID(), entity.getTitle());
        context.event().fireDeviceStatus("firmata", entity);
        for (Pin pin : device.getIoDevice().getPins()) {
          String pinLabel = PinoutModel.getPinLabel(entity.getBoard(), pin.getIndex());
          if (!entity.getExcludePins().contains(pinLabel)) {
            var ep = new FirmataPinEndpoint(pinLabel, pin, entity, FirmataService.this);
            endpoints.put(String.valueOf(pin.getIndex()), ep);
          }
        }

        // send init a message to tell a device that server wants to listen to it
        try {
          device.sendMessage(new byte[]{START_SYSEX, SYSEX_INIT, END_SYSEX});
        } catch (Exception e) {
          log.error("[{}]: Unable to send init message to Firmata device: <{}>", entity.getEntityID(), entity.getTitle(), e);
        }
      }
    };
  }

  @Override
  public void entityUpdated(@NotNull FirmataBaseEntity<?> newEntity) {
    super.entityUpdated(newEntity);
    if (endpoints != null) {
      for (FirmataPinEndpoint endpoint : endpoints.values()) {
        endpoint.setEntity(newEntity);
      }
      for (String excludePin : entity.getExcludePins()) {
        endpoints.values().removeIf(pin -> {
          if (pin.getPinLabel().equals(excludePin)) {
            pin.removePin();
            return true;
          }
          return false;
        });
      }
    }
  }

  @Override
  public void destroy(boolean forRestart, @Nullable Exception ex) {
    if (ioDevice != null && ioDevice.isReady()) {
      try {
        ioDevice.stop();
      } catch (Exception se) {
        log.warn("[{}]: Unable to stop firmata communicator: <{}>",
          entity.getEntityID(), se.getMessage());
      }
    }
    ioDevice = null;
  }

  @Override
  @SneakyThrows
  protected void initialize() {
    if (entity instanceof FirmataNetworkEntity networkEntity) {
      ContextNetwork.ping(networkEntity.getIp(), networkEntity.getPort());
    } else if (entity instanceof FirmataUsbEntity usbEntity) {
      if (usbEntity.getSerialPort() == null) {
        throw new IllegalArgumentException("Port '" + usbEntity.getJsonData("serialPort") + "' is unreachable");
      }
    }
    String result = restart();
    if (result.endsWith("_error")) {
      context.ui().toastr().error(result);
    } else {
      context.ui().toastr().success(result);
    }
  }

  public Long getDeviceStartedTime() {
    return getTimeIfValid(() -> deviceStartTime);
  }

  public Long getLastPingResponseTime() {
    return getTimeIfValid(() -> lastPingResponseTime);
  }

  public Long getLastInitResponseTime() {
    return getTimeIfValid(() -> lastInitResponseTime);
  }

  public Long getWatchDogTimeoutToRestart() {
    return getTimeIfValid(() -> {
      long time = lastPingResponseTime;
      if (time > 0) {
        return MINUTES.toMillis(entity.getWatchDogTimeout()) - (System.currentTimeMillis() - time);
      }
      return 0L;
    });
  }


  private @Nullable Long getTimeIfValid(@NotNull Supplier<Long> timeSupplier) {
    Long time = timeSupplier.get();
    return (time != null && time != 0) ? time : null;
  }

  @Override
  public String isRequireRestartService() {
    if (entity.getStatus() == Status.ERROR) {
      return "Firmata device error";
    } else if (entity.getStatus().isOnline()) {
      long time = lastPingResponseTime;
      if (time > 0 && time + MINUTES.toMillis(entity.getWatchDogTimeout()) < System.currentTimeMillis()) {
        return "Firmata device no ping responses";
      }
    }
    return null;
  }

  public final String restart() {
    // skip restart if status ONLINE
    if (entity.getStatus().isOnline()) {
      return "action.communicator.already_run";
    }
    log.info("[{}]: Restarting Firmata device: {} for device type: {}",
      entity.getEntityID(), entity.getTitle(), getClass().getSimpleName());

    // try restart seldom that once per minute
    if (System.currentTimeMillis() - lastRestartAttempt < 30000) {
      return "action.communicator.restart_too_often_error";
    }
    lastRestartAttempt = System.currentTimeMillis();

    try {
      destroy(true, null);
      startAndConnectToDevice();
      return "action.communicator.success";
    } catch (Exception ex) {
      destroy(false, ex);
      entity.setStatusError(ex);
      log.error("[{}]: Error while initialize device: {} for device type: {}",
        entity.getEntityID(), entity.getTitle(), getClass().getSimpleName(), ex);
      return "action.communicator.unknown_error";
    }
  }

  public void startAndConnectToDevice() throws Exception {
    ioDevice = createIODevice();
    device = new IODeviceWrapper(ioDevice, this);
    ioDevice.addProtocolMessageHandler(SYSEX_CUSTOM_MESSAGE, new Consumer<>() {

      @Override
      public void accept(Event event) {
        // currently not works
        ByteBuffer payload = ByteBuffer.wrap((byte[]) event.getBodyItem(SYSEX_CUSTOM_MESSAGE));
        byte commandID = payload.get();
        switch (commandID) {
          case SYSEX_INIT:
            log.info("[{}]: Firmata device <{}> communication initialized",
              entity.getEntityID(), entity.getTitle());
            lastInitResponseTime = System.currentTimeMillis();
            break;
          case SYSEX_PING:
            log.info("[{}]: Firmata device <{}> communication pinged",
              entity.getEntityID(), entity.getTitle());
            lastPingResponseTime = System.currentTimeMillis();
            break;
          default:
            // this piece now isn't used
            FirmataCommandPlugin handler = firmataCommandPlugins.getFirmataCommandPlugin(commandID);
            if (handler == null) {
              log.error("[{}]: Unable to find command: {}", entity.getEntityID(), commandID);
            } else {
              byte messageID = handler.hasTH() ? HomioFirmataUtil.getByte(payload) : 0;
              // short target = handler.hasTH() ? HomioFirmataUtil.getShort(payload) : 0;
              handler.handle(device, entity, messageID, payload);
              /*if (entity.getJoined() == Status.ONLINE) {
                handler.handle(device, entity, messageID, payload);
              } else if (entity.getDeviceID() == target || handler.isHandleBroadcastEvents()) {
                handler.broadcastHandle(device, entity, messageID, target, payload);
              }
              log.warn("[{}]: Firmata device <{}> received unknown command: <{}>",
                entity.getEntityID(), entity.getTitle(), commandID);*/
            }
        }
      }
    });
    ioDevice.addEventListener(this);
    ioDevice.start();

    // this method throws exception if unable to get any notifications from the device
    ioDevice.ensureInitializationIsDone();
    String deviceInfo = ioDevice.getProtocol().split(" - ")[0];
    if (!deviceInfo.startsWith("hf:")) {
      throw new IllegalStateException("Could not find hf: in device info: " + deviceInfo);
    }
    String[] data = deviceInfo.split(":");
    DeviceInfo info = new DeviceInfo(data[1], data[2], data[3], Long.parseLong(data[4]));
    afterStartHandler.accept(info);
  }

  private IODevice createIODevice() {
    if (entity instanceof FirmataUsbEntity usbEntity) {
      return new FirmataDevice(usbEntity.getSerialPort().getSystemPortName());
    } else if (entity instanceof FirmataNetworkEntity networkEntity) {
      return new FirmataDevice(new NetworkTransport(networkEntity.getIp() + ":" + networkEntity.getPort()));
    }
    throw new IllegalStateException("Unable to create Firmata device for entity: " + entity.getClass().getSimpleName());
  }

  @Override
  public void onStart(IOEvent event) {
    log.info("[{}]: Firmata device: {} communication started", entity.getEntityID(), entity.getTitle());
  }

  @Override
  public void onStop(IOEvent event) {
    log.info("[{}]: Firmata device: {} communication stopped", entity.getEntityID(), entity.getTitle());
  }

  @Override
  public void onPinChange(IOEvent event) {
    if (entity.getDeviceID() != -1) {
      context.event().fireEvent(entity.getDeviceID() + "-pin-" + event.getPin().getIndex(),
        new DecimalType(event.getPin().getValue()));
    }
  }

  @Override
  public void onMessageReceive(IOEvent event, String message) {
    log.info("[{}]: Firmata <{}> got message: <{}>", entity.getEntityID(), entity.getTitle(), message);
  }

  @SneakyThrows
  public void sendMessage(byte[] data) {
    try {
      device.sendMessage(data);
    } catch (SocketException ex) {
      log.error("[{}]: Unable to send message to Firmata device: <{}>", entity.getEntityID(), entity.getTitle(), ex);
      entity.setStatusError(ex);
    }
  }

  public Pin getPin(Integer pinNum) {
    return device.getIoDevice().getPin(pinNum);
  }

  public boolean isReady() {
    return device.getIoDevice().isReady();
  }

  public String getProtocol() {
    return device.getIoDevice().getProtocol();
  }

  public void sendOneWireDelay(byte pin, int delay) {
    device.getIoOneWire().sendOneWireDelay(pin, delay);
  }

  public void sendOneWireConfig(byte pin, boolean enableParasiticPower) {
    device.getIoOneWire().sendOneWireConfig(pin, enableParasiticPower);
  }

  public void sendOneWireWrite(byte pin, ByteBuffer address, ByteBuffer data, Integer delay, boolean reset) {
    device.getIoOneWire().sendOneWireWrite(pin, address, data, delay, reset);
  }

  public byte[] sendOneWireWriteAndRead(byte pin, ByteBuffer address, ByteBuffer data, byte numBytesToRead, Integer delay, boolean reset) {
    return device.getIoOneWire().sendOneWireWriteAndRead(pin, address, data, numBytesToRead, delay, reset);
  }

  @SneakyThrows
  public I2CDevice getI2CDevice(byte address) {
    return device.getIoDevice().getI2CDevice(address);
  }

  public void pingDevice() {
    if (System.currentTimeMillis() - lastPingRequestTime > MINUTES.toMillis(entity.getPingInterval())) {
      sendMessage(new byte[]{START_SYSEX, SYSEX_PING, END_SYSEX});
      lastPingRequestTime = System.currentTimeMillis();
    }
  }

  public List<OneWireDevice> sendOneWireSearch(byte pinNum) {
    return device.getIoOneWire().sendOneWireSearch(pinNum);
  }

  public Set<Pin> getPins() {
    return device.getIoDevice().getPins();
  }

  public record DeviceInfo(String board, String deviceID, String name, long started) {
  }

}
