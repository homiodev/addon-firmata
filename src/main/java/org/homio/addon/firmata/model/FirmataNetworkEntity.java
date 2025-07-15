package org.homio.addon.firmata.model;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.UIFieldPort;
import org.homio.api.ui.field.UIFieldType;
import org.homio.api.ui.route.UIRouteMicroController;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static org.homio.addon.firmata.FirmataNetworkControllerScanner.FIRMATA_PORT;

@Entity
@NoArgsConstructor
@UIRouteMicroController(icon = "fas fa-microchip", color = "#27966E")
public final class FirmataNetworkEntity extends FirmataBaseEntity<FirmataNetworkEntity> {

  public FirmataNetworkEntity(@NotNull String hostAddress) {
    setIp(hostAddress);
  }

  @UIField(order = 22, type = UIFieldType.IpAddress)
  public String getIp() {
    return getJsonData("ip");
  }

  public FirmataNetworkEntity setIp(String ip) {
    setJsonData("ip", ip);
    return this;
  }

  @UIField(order = 23)
  @UIFieldPort
  public int getPort() {
    return getJsonData("port", FIRMATA_PORT);
  }

  public void setPort(int port) {
    setJsonData("port", port);
  }

  @Override
  protected String getCommunicatorName() {
    return "WIFI";
  }

  @Override
  protected @NotNull String getDevicePrefix() {
    return "firmatanet";
  }

  @Override
  public long getEntityServiceHashCode() {
    return getJsonDataHashCode("ip", "port");
  }

  @Override
  protected void assembleMissingMandatoryFields(@NotNull Set<String> fields) {
    if (getIp().isEmpty()) {
      fields.add("ip");
    }
  }
}
