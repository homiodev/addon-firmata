package org.homio.bundle.firmata.model;

import com.fazecast.jSerialComm.SerialPort;
import javax.persistence.Entity;
import org.apache.commons.lang3.StringUtils;
import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.converter.serial.JsonSerialPort;
import org.homio.bundle.api.converter.serial.SerialPortDeserializer;
import org.homio.bundle.api.optionProvider.SelectSerialPortOptionLoader;
import org.homio.bundle.api.ui.field.UIField;
import org.homio.bundle.api.ui.field.selection.UIFieldSelectValueOnEmpty;
import org.homio.bundle.api.ui.field.selection.UIFieldSelection;
import org.homio.bundle.firmata.provider.FirmataDeviceCommunicator;
import org.homio.bundle.firmata.provider.command.PendingRegistrationContext;

@Entity
public final class FirmataUsbEntity extends FirmataBaseEntity<FirmataUsbEntity> {

  public static final String PREFIX = "fmusb_";

  @UIField(order = 22)
  @JsonSerialPort
  @UIFieldSelection(SelectSerialPortOptionLoader.class)
  @UIFieldSelectValueOnEmpty(label = "selection.serialPort", color = "#A7D21E")
  public SerialPort getSerialPort() {
    String serialPort = getJsonData("serialPort");
    return SerialPortDeserializer.getSerialPort(serialPort);
  }

  public void setSerialPort(SerialPort serialPort) {
    setJsonData("serialPort", serialPort == null ? "" : serialPort.getSystemPortName());
  }

  @Override
  protected String getCommunicatorName() {
    return "SERIAL";
  }

  @Override
  public FirmataDeviceCommunicator createFirmataDeviceType(EntityContext entityContext) {
    SerialPort serialPort = getSerialPort();
    return serialPort == null ? null : new FirmataUSBFirmataDeviceCommunicator(entityContext, this, serialPort.getSystemPortName());
  }

  @Override
  protected boolean allowRegistrationType(PendingRegistrationContext pendingRegistrationContext) {
    return pendingRegistrationContext.getEntity() instanceof FirmataUsbEntity;
  }

  @Override
  public String getEntityPrefix() {
    return PREFIX;
  }

  private static class FirmataUSBFirmataDeviceCommunicator extends FirmataDeviceCommunicator<FirmataUsbEntity> {

    private final String port;

    public FirmataUSBFirmataDeviceCommunicator(EntityContext entityContext, FirmataUsbEntity entity, String port) {
      super(entityContext, entity);
      this.port = port;
    }

    @Override
    protected IODevice createIODevice(FirmataUsbEntity entity) {
      return StringUtils.isEmpty(port) ? null : new FirmataDevice(port);
    }

    @Override
    public long generateUniqueIDOnRegistrationSuccess() {
      return 1;
    }
  }
}
