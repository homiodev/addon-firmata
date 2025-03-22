package org.homio.addon.firmata.model;

import com.fazecast.jSerialComm.SerialPort;
import jakarta.persistence.Entity;
import org.homio.api.converter.serial.JsonSerialPort;
import org.homio.api.converter.serial.SerialPortDeserializer;
import org.homio.api.optionProvider.SelectSerialPortOptionLoader;
import org.homio.api.ui.UISidebarChildren;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.selection.UIFieldSelectConfig;
import org.homio.api.ui.field.selection.dynamic.UIFieldDynamicSelection;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@SuppressWarnings({"JpaAttributeTypeInspection", "unused", "rawtypes"})
@Entity
@UISidebarChildren(icon = "fas fa-microchip", color = "#27966E")
public final class FirmataUsbEntity extends FirmataBaseEntity<FirmataUsbEntity> {

  @Override
  protected @NotNull String getDevicePrefix() {
    return "firmatausb";
  }

  @UIField(order = 22)
  @JsonSerialPort
  @UIFieldDynamicSelection(SelectSerialPortOptionLoader.class)
  @UIFieldSelectConfig(selectOnEmptyLabel = "SELECTION.serialPort", iconColor = "#A7D21E")
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
  public long getEntityServiceHashCode() {
    return getJsonDataHashCode("serialPort");
  }

  @Override
  protected void assembleMissingMandatoryFields(@NotNull Set<String> fields) {
    if (getJsonData("serialPort").isEmpty()) {
      fields.add("serialPort");
    }
  }
}
