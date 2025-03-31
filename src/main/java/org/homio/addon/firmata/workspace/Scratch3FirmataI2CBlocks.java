package org.homio.addon.firmata.workspace;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
//@Component
/**
 * For now it's too much work to implement i2c devices
 * TODO: Use https://github.com/mattjlewis/diozero for adding new devices
 */
public class Scratch3FirmataI2CBlocks /*extends Scratch3BaseDeviceBlocks<FirmataBaseEntity<?>>*/ {

  /*private static final int BME280_ADDRESS = 0x76; // Use 0x77 if needed
  private static final int REG_CONFIG = 0xF5;
  private static final int REG_CTRL_HUM = 0xF2;
  private static final int REG_CTRL_MEAS = 0xF4;
  private static final int REG_DATA = 0xF7; // Start of data registers

  private final MenuBlock.StaticMenuBlock<BME280ValueMenu> bme280ValueMenu;

  public Scratch3FirmataI2CBlocks(Context context, FirmataEntrypoint firmataEntrypoint) {
    super("#E0D225", context, firmataEntrypoint, "ic");

    this.bme280ValueMenu = menuStatic("bme280ValueMenu", BME280ValueMenu.class, BME280ValueMenu.Temp);

    blockReporter(9, "BME280", "BME280 [TYPE] of [DEVICE]", this::getBME280ValueEvaluate, block -> {
      block.addArgument(DEVICE, this.deviceMenu);
      block.addArgument("TYPE", this.bme280ValueMenu);
    });
  }

  private State getBME280ValueEvaluate(WorkspaceBlock workspaceBlock) {
    BME280ValueMenu type = workspaceBlock.getMenuValue("TYPE", this.bme280ValueMenu);
    return executeWhenDeviceReady(workspaceBlock, entity -> {
      try {
        I2CDevice bme280 = entity.getService().getI2CDevice((byte) 0x77);
        // Configure BME280 (example settings: humidity x1, temp/pressure x1, normal mode)
        // Write to register 0xF2 (CTRL_HUM): set humidity oversampling
        bme280.tell((byte) REG_CTRL_HUM, (byte) 0x01);
        // Write to register 0xF4 (CTRL_MEAS): set temp/pressure oversampling and mode
        bme280.tell((byte) REG_CTRL_MEAS, (byte) 0x27);

        // Wait for the sensor to stabilize (adjust delay as needed)
        Thread.sleep(100);

        // Read data from registers 0xF7 to 0xFE (pressure, temp, humidity)
        byte[] data = bme280.read(REG_DATA, 8); // Read 8 bytes starting at 0xF7

        // Parse raw sensor data (example for temperature)
        int tempRaw = ((data[3] & 0xFF) << 12) | ((data[4] & 0xFF) << 4) | ((data[5] & 0xFF) >> 4);
        double temperature = compensateTemperature(tempRaw); // Implement calibration
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private enum BME280ValueMenu {
    Temp, Pressure, Humidity
  }*/
}
