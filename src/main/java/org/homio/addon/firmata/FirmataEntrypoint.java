package org.homio.addon.firmata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.firmata4j.Consumer;
import org.homio.addon.firmata.arduino.ArduinoConfiguration;
import org.homio.addon.firmata.arduino.ArduinoConsolePlugin;
import org.homio.addon.firmata.arduino.setting.ConsoleArduinoLibraryManagerSetting;
import org.homio.addon.firmata.model.FirmataBaseEntity;
import org.homio.addon.firmata.model.FirmataNetworkEntity;
import org.homio.addon.firmata.model.FirmataService;
import org.homio.addon.firmata.provider.FirmataCommandPlugins;
import org.homio.api.AddonConfiguration;
import org.homio.api.AddonEntrypoint;
import org.homio.api.Context;
import org.homio.api.setting.SettingPluginPackageInstall;
import org.homio.api.util.CommonUtils;
import org.homio.api.util.Lang;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Getter
@Log4j2
@Component
@AddonConfiguration
@RequiredArgsConstructor
public class FirmataEntrypoint implements AddonEntrypoint {

  private final Context context;
  private final ArduinoConsolePlugin arduinoConsolePlugin;
  private final FirmataCommandPlugins firmataCommandPlugins;

  public static boolean firmataFoundFromScanner(@NotNull Context context, @NotNull String hostAddress) {
    AtomicBoolean found = new AtomicBoolean(false);
    try {
      // When we detected firmata ip address, we have to check if this device already existed but changed IP address
      FirmataNetworkEntity temp = new FirmataNetworkEntity(hostAddress);
      temp.setEntityID("TempFirmata" + System.currentTimeMillis());
      FirmataService service = new FirmataService(context, temp, false);
      service.setAfterStartHandler(new Consumer<>() {
        @Override
        public void accept(FirmataService.DeviceInfo deviceInfo) {
          found.set(foundController(context, deviceInfo.board(), deviceInfo.deviceID(), hostAddress));
        }
      });
      service.startAndConnectToDevice();
      service.destroy(false, null);
    } catch (Exception ignored) {
    }
    return found.get();
  }

  // this method fires only from devices that support internet access
  public static boolean foundController(@NotNull Context context,
                                        @NotNull String board,
                                        @NotNull String deviceID,
                                        @NotNull String hostAddress) {
    // check if we already have firmata device with deviceID
    var device = context.db().findAll(FirmataBaseEntity.class).stream()
      .filter(d -> Objects.equals(d.getIeeeAddress(), deviceID))
      .findAny().orElse(null);

    if (device != null) {
      if (device instanceof FirmataNetworkEntity ae) {
        if (!hostAddress.equals(ae.getIp())) {
          context.ui().toastr().warn("FIRMATA.EVENT.CHANGED_IP",
            Map.of("DEVICE", device.getTitle(), "OLD", ae.getIp(), "NEW", hostAddress));
          // update device ip address and try to restart it
          context.db().save(ae.setIp(hostAddress));
        } else {
          log.info("Firmata device <{}> up to date.", device.getTitle());
        }
      } else {
        context.ui().toastr().warn("FIRMATA.EVENT.FIRMATA_WRONG_DEVICE_TYPE", Map.of("ID",
          deviceID, "NAME", device.getTitle()));
      }
      return false;
    } else {
      List<String> messages = new ArrayList<>();
      messages.add(Lang.getServerMessage("FIRMATA.NEW_DEVICE_QUESTION"));
      messages.add(Lang.getServerMessage("FIRMATA.NEW_DEVICE_BOARD", "BOARD", board));
      messages.add(Lang.getServerMessage("FIRMATA.NEW_DEVICE_ID", "DEVICE_ID", deviceID));
      messages.add(Lang.getServerMessage("FIRMATA.NEW_DEVICE_ADDRESS", "ADDRESS", hostAddress));

      context.ui().dialog().sendConfirmation("Confirm-Firmata-" + deviceID, "TITLE.NEW_DEVICE", () -> {
        // save a device and try to restart it
        var entity = new FirmataNetworkEntity(hostAddress);
        entity.setBoard(board);
        entity.setIeeeAddress(deviceID);
        context.db().save(entity);
      }, messages, null);
      return true;
    }
  }

  public void init() {
    var commonFiles = getClass().getResource("/arduino-files-common.7z");
    if (commonFiles == null) {
      // throw new IllegalStateException("Could not find arduino-files-common.7z");
    }
    var osFiles = getClass().getResource("/arduino-files-os.7z");
    if (osFiles == null) {
      // throw new IllegalStateException("Could not find arduino-files-os.7z");
    }
    var arduinoInstallPath = CommonUtils.getInstallPath().resolve("arduino");
    context.bgp().runWithProgress("arduino-install").execute(progressBar -> {
      try {
        /*ArchiveUtil.unzip(commonFiles, "arduino-files-common.7z", arduinoInstallPath,
          null, false, progressBar, ArchiveUtil.UnzipFileIssueHandler.skip);
        ArchiveUtil.unzip(osFiles, "arduino-files-os.7z", arduinoInstallPath,
          null, false, progressBar, ArchiveUtil.UnzipFileIssueHandler.skip);*/

        // fire install ConfigurableFirmata library
        var firmataLib = new SettingPluginPackageInstall.PackageRequest();
        firmataLib.setName("ConfigurableFirmata");
        firmataLib.setVersion("3.3.0");
        new ConsoleArduinoLibraryManagerSetting()
          .installPackage(context, firmataLib, progressBar);

        initInternal();
      } catch (Exception e) {
        log.error("Could not initialize arduino", e);
        throw new RuntimeException(e);
      }
    });
  }

  private void initInternal() {
    ArduinoConfiguration.getPlatform();
    arduinoConsolePlugin.init();

    this.context.network().listenUdp("listen-firmata-udp", null, 8266, (datagramPacket, payload) -> {
      if (payload.startsWith("hf:")) {
        String[] parts = payload.split(":");
        if (parts.length == 3) {
          var hostAddress = datagramPacket.getAddress().getHostAddress();
          foundController(context, parts[1].trim(), parts[2].trim(), hostAddress);
        }
      }
    });

    // ping firmata device if live status is online
    this.context.bgp().builder("firmata-device-ping")
      .delay(Duration.ofMinutes(1))
      .interval(Duration.ofMinutes(1))
      .execute(() -> {
        for (FirmataBaseEntity<?> firmataBaseEntity : context.db().findAll(FirmataBaseEntity.class)) {
          if (firmataBaseEntity.getStatus().isOnline()) {
            firmataBaseEntity.getService().pingDevice();
          }
        }
      });
  }
}
