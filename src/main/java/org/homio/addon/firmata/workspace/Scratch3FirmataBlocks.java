package org.homio.addon.firmata.workspace;

import com.pivovarit.function.ThrowingBiConsumer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.firmata4j.Pin;
import org.firmata4j.firmata.FirmataMessageFactory;
import org.homio.addon.firmata.FirmataEntrypoint;
import org.homio.addon.firmata.model.FirmataBaseEntity;
import org.homio.addon.firmata.provider.util.OneWireDevice;
import org.homio.api.Context;
import org.homio.api.model.OptionModel;
import org.homio.api.state.DecimalType;
import org.homio.api.state.State;
import org.homio.api.workspace.WorkspaceBlock;
import org.homio.api.workspace.scratch.MenuBlock;
import org.homio.api.workspace.scratch.Scratch3BaseDeviceBlocks;
import org.homio.api.workspace.scratch.Scratch3Block;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.function.BiPredicate;
import java.util.function.Function;

@Log4j2
@Getter
@Component
public class Scratch3FirmataBlocks extends Scratch3BaseDeviceBlocks<FirmataBaseEntity<?>> {

  public static final String ALL_REST_PIN = "rest/firmata/pin";
  public static final String REST_PIN = "rest/firmata/pin/";
  public static final String PIN = "PIN";
  public static final String ONE_REST = "rest/firmata/onewire/address?family=";
  private final MenuBlock.StaticMenuBlock<CompareType> menuOp;
  private final MenuBlock.StaticMenuBlock<Pin.Mode> menuPinMode;
  private final MenuBlock.ServerMenuBlock menuPinDigital;
  private final MenuBlock.ServerMenuBlock menuPinPwm;
  private final MenuBlock.ServerMenuBlock menuPinAll;
  private final MenuBlock.ServerMenuBlock menuPinServo;
  private final MenuBlock.ServerMenuBlock pinMenu1Wire;
  private final MenuBlock.ServerMenuBlock menuTemperatureAddress;

  private final Scratch3Block ds18b20Value;
  boolean sendConfig = false;

  public Scratch3FirmataBlocks(Context context, FirmataEntrypoint firmataEntrypoint) {
    super("#3B7470", context, firmataEntrypoint, "firmata");

    this.pinMenu1Wire = menuServer(PIN, REST_PIN + Pin.Mode.ONEWIRE, "1-Wire").setDependency(deviceMenu);
    this.menuTemperatureAddress = menuServer("pinMenu1WireAddress",
      ONE_REST + ONE_WIRE.DS18B20.TEMPERATURE_FAMILY, "Temperature address")
      .setDependency(deviceMenu, this.pinMenu1Wire);

    this.ds18b20Value = blockReporter(10, "DS18B20",
      "DS18B20(1-Wire) on [PIN] address [ADDRESS] of [FIRMATA]", this::getDS18B20Value, block ->
        addPinMenu(block, this.pinMenu1Wire));
    this.ds18b20Value.addArgument("ADDRESS", this.menuTemperatureAddress);

    // Menu
    this.menuPinDigital = menuServer("pinMenuDigital", REST_PIN + Pin.Mode.OUTPUT, "Digital pin").setDependency(deviceMenu);
    this.menuPinPwm = menuServer("pinMenuPwm", REST_PIN + Pin.Mode.PWM, "Pwm pin").setDependency(deviceMenu);
    this.menuPinServo = menuServer("pinMenuServo", REST_PIN + Pin.Mode.SERVO, "Servo pin").setDependency(deviceMenu);
    this.menuPinAll = menuServer("pinMenuAll", ALL_REST_PIN, "Pin").setDependency(deviceMenu);

    this.menuOp = menuStatic("opMenu", CompareType.class, CompareType.GREATER);
    this.menuPinMode = menuStatic("pinModeMenu", Pin.Mode.class, Pin.Mode.OUTPUT);

    // Blocks
    blockReporter(5, "pin_read", "Get [PIN] of [DEVICE]", this::readPinEvaluate, block ->
      addPinMenu(block, this.menuPinAll));

    blockCommand(10, "digital_write", "Set(D) Pin [PIN] [ON_OFF] to [DEVICE]", this::digitalWriteHandler, block -> {
      block.addArgument("ON_OFF", getOnOffMenu());
      addPinMenu(block, this.menuPinDigital);
    });

    blockCommand(20, "pwm_write", "Set(PWM) Pin [PIN] [VALUE] to [DEVICE]", this::pwmWriteHandler, block -> {
      block.addArgument(VALUE, 50);
      addPinMenu(block, this.menuPinPwm);
    });

    blockCommand(25, "invert_pin", "Invert Pin(D) [PIN] of [DEVICE]", this::invertPinHandler, block ->
      addPinMenu(block, this.menuPinDigital));

    blockHat(45, "when_pin_changed", "When Pin [PIN] changed of [DEVICE]", this::whenPinChangedHandler, block ->
      addPinMenu(block, this.menuPinAll));

    blockHat(50, "when_pin_op_value", "When Pin [PIN] [OP] value [VALUE] of [DEVICE]", this::whenPinOpValueHandler, block -> {
      block.addArgument("OP", this.menuOp);
      block.addArgument(VALUE, 0);
      addPinMenu(block, this.menuPinAll);
    });

    blockCommand(60, "set_mode", "Set Pin [PIN] mode [MODE] to [DEVICE]", this::setPinModeHandler, block -> {
      block.addArgument("MODE", this.menuPinMode);
      addPinMenu(block, this.menuPinPwm);
    });

    blockCommand(65, "set_sampling_interval", "Set Sampling [INTERVAL] of [DEVICE]", this::setSamplingIntervalHandler, block -> {
      block.addArgument(DEVICE, deviceMenu);
      block.addArgument("INTERVAL", 19);
    });

    blockCommand(70, "set_servo_config", "Servo pulse min/max [MIN]/[MAX] of [DEVICE]", this::setServoConfigHandler, block -> {
      block.addArgument("MIN", 0);
      block.addArgument("MAX", "---");
      addPinMenu(block, this.menuPinServo);
    });

    blockCommand(70, "delay", "Delay [VALUE] of [DEVICE]", this::delayHandler, block -> {
      block.addArgument(DEVICE, deviceMenu);
      block.addArgument(VALUE, 3);
    });
  }

  static Integer getPin(WorkspaceBlock workspaceBlock, MenuBlock.ServerMenuBlock menuBlock) {
    String pinNum = workspaceBlock.getMenuValue(PIN, menuBlock);
    return Integer.valueOf(pinNum);
  }

  private void delayHandler(WorkspaceBlock workspaceBlock) {
    int value = workspaceBlock.getInputInteger(VALUE);
    executeWhenDeviceReady(workspaceBlock, entity -> {
      entity.getService().sendOneWireDelay((byte) 0, value);
    });
  }

  private void setServoConfigHandler(WorkspaceBlock workspaceBlock) {
    executeNoResponse(workspaceBlock, this.menuPinAll, (entity, pin) -> {
      int minPulse = workspaceBlock.getInputInteger("MIN");
      int maxPulse = workspaceBlock.getInputInteger("MAX");
      pin.setServoMode(minPulse, maxPulse);
    });
  }

  private void setSamplingIntervalHandler(WorkspaceBlock workspaceBlock) {
    int interval = workspaceBlock.getInputInteger("INTERVAL");
    executeWhenDeviceReady(workspaceBlock, entity -> {
      entity.getService().sendMessage(FirmataMessageFactory.setSamplingInterval(interval));
    });
  }

  private void setPinModeHandler(WorkspaceBlock workspaceBlock) {
    Pin.Mode mode = workspaceBlock.getMenuValue("MODE", this.menuPinMode);
    executeNoResponse(workspaceBlock, this.menuPinAll, (entity, pin) -> pin.setMode(mode));
  }

  private void whenPinChangedHandler(WorkspaceBlock workspaceBlock) {
    whenPinChangedHandler(workspaceBlock, value -> true);
  }

  private void whenPinOpValueHandler(WorkspaceBlock workspaceBlock) {
    CompareType compareType = workspaceBlock.getMenuValue("OP", this.menuOp);
    Integer compareValue = workspaceBlock.getInputInteger(VALUE);
    whenPinChangedHandler(workspaceBlock, value -> compareType.match((long) value, compareValue));
  }

  private void whenPinChangedHandler(WorkspaceBlock workspaceBlock, Function<Object, Boolean> checkFn) {
    workspaceBlock.handleNextOptional(next ->
      executeWhenDeviceReady(workspaceBlock, entity -> {
        var pinIndex = getPin(workspaceBlock, menuPinAll);
        var pin = entity.getService().getPin(pinIndex);
        var lock = workspaceBlock.getLockManager().getLock(workspaceBlock, entity.getDeviceID() + "-pin-" + pin.getIndex());
        workspaceBlock.subscribeToLock(lock, checkFn, next::handle);
      }));
  }

  private State readPinEvaluate(WorkspaceBlock workspaceBlock) {
    return executeWhenDeviceReady(workspaceBlock, (Function<FirmataBaseEntity<?>, State>) entity -> {
      var pin = getPin(workspaceBlock, menuPinAll);
      return new DecimalType(pin.longValue());
    });
  }

  private void digitalWriteHandler(WorkspaceBlock workspaceBlock) {
    updatePinValue(workspaceBlock, Pin.Mode.OUTPUT, pin ->
      (long) workspaceBlock.getMenuValue("ON_OFF", getOnOffMenu()).ordinal());
  }

  private void pwmWriteHandler(WorkspaceBlock workspaceBlock) {
    updatePinValue(workspaceBlock, Pin.Mode.PWM, pin -> workspaceBlock.getInputInteger(VALUE).longValue());
  }

  private void invertPinHandler(WorkspaceBlock workspaceBlock) {
    updatePinValue(workspaceBlock, Pin.Mode.OUTPUT, pin -> pin.getValue() == 1 ? 0L : 1L);
  }

  private void updatePinValue(WorkspaceBlock workspaceBlock, Pin.Mode mode, Function<Pin, Long> pinValueProducer) {
    executeNoResponse(workspaceBlock, this.menuPinDigital, (entity, pin) -> {
      pin.setMode(mode);
      Long value = pinValueProducer.apply(pin);
      pin.setValue(value);
    });
  }

  void addPinMenu(Scratch3Block scratch3Block, MenuBlock.ServerMenuBlock pinMenuBlock) {
    scratch3Block.addArgument(DEVICE, deviceMenu);
    scratch3Block.addArgument(PIN, pinMenuBlock);
  }

  void executeNoResponse(WorkspaceBlock workspaceBlock, MenuBlock.ServerMenuBlock pinMenuBlock,
                         ThrowingBiConsumer<FirmataBaseEntity<?>, Pin, Exception> consumer) {
    executeWhenDeviceReady(workspaceBlock, entity -> {
      try {
        consumer.accept(entity, entity.getService().getPin(getPin(workspaceBlock, pinMenuBlock)));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private State getDS18B20Value(WorkspaceBlock workspaceBlock) {
    Long longAddress = workspaceBlock.getMenuValue("ADDRESS", this.menuTemperatureAddress, Long.class);
    ByteBuffer address = OneWireDevice.toByteArray(longAddress);

    Integer pinNum = pinMenu1Wire == null ? null : getPin(workspaceBlock, pinMenu1Wire);
    FirmataBaseEntity<?> entity = workspaceBlock.getMenuValueEntityRequired(DEVICE, deviceMenu);
    var pin = entity.getService().getPin(pinNum);

    if (entity.getStatus().isOnline()) {
      entity.getService().sendOneWireConfig(pin.getIndex(), true);

      // start conversion, with parasite power on at the end
      ByteBuffer payload = ByteBuffer.allocate(1).put(ONE_WIRE.DS18B20.CONVERT_TEMPERATURE_COMMAND);
      entity.getService().sendOneWireWrite(pin.getIndex(), address, payload, null, true);

      // maybe 750 ms is enough, maybe not
      entity.getService().sendOneWireDelay(pin.getIndex(), 1);

      // Read Scratchpad
      payload = ByteBuffer.allocate(1).put(ONE_WIRE.DS18B20.READ_SCRATCHPAD_COMMAND);
      byte[] data = entity.getService()
        .sendOneWireWriteAndRead(pin.getIndex(), address, payload, ONE_WIRE.DS18B20.READ_COUNT, null, true);
      float value = (float) (data == null ? -1 : ((data[1] & 0xFF) << 8) | data[0] & 0xFF) / 16;
      return new DecimalType(value);
    }
    return null;
  }

  @AllArgsConstructor
  public enum CompareType implements OptionModel.KeyValueEnum {
    GREATER(">", (a, b) -> a > b),
    LESS("<", (a, b) -> a < b),
    GREATER_EQUAL(">=", (a, b) -> a >= b),
    LESS_EQUAL("<=", (a, b) -> a <= b),
    EQUAL("=", (a, b) -> Double.compare(a, b) == 0);

    @Getter
    private final String shortName;

    private final BiPredicate<Double, Double> matchFn;

    public boolean match(double a, double b) {
      return matchFn.test(a, b);
    }

    @Override
    public String toString() {
      return shortName;
    }
  }

  private static class ONE_WIRE {

    private static class DS18B20 {

      private static final byte TEMPERATURE_FAMILY = 0x28;
      private static final byte CONVERT_TEMPERATURE_COMMAND = 0x44;
      private static final byte READ_SCRATCHPAD_COMMAND = (byte) 0xBE;
      private static final byte READ_COUNT = 2;
    }
  }
}
