package org.touchhome.bundle.firmata.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.validation.constraints.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.transport.NetworkTransport;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.model.OptionModel;
import org.touchhome.bundle.api.ui.action.DynamicOptionLoader;
import org.touchhome.bundle.api.ui.field.UIField;
import org.touchhome.bundle.api.ui.field.selection.UIFieldSelectValueOnEmpty;
import org.touchhome.bundle.api.ui.field.selection.UIFieldSelection;
import org.touchhome.bundle.firmata.FirmataBundleEntrypoint;
import org.touchhome.bundle.firmata.provider.FirmataDeviceCommunicator;
import org.touchhome.bundle.firmata.provider.command.PendingRegistrationContext;

@Entity
public final class FirmataNetworkEntity extends FirmataBaseEntity<FirmataNetworkEntity> {

  public static final String PREFIX = "fmntw_";

  @UIField(order = 22)
  @Pattern(regexp = "(\\d{1,3}\\.){3}\\d{1,3}", message = "validation.host_port")
  @UIFieldSelection(value = SelectFirmataIpDeviceLoader.class, allowInputRawText = true)
  @UIFieldSelectValueOnEmpty(label = "selection.selectIp", color = "#A7D21E")
  public String getIp() {
    return getJsonData("ip");
  }

  public FirmataNetworkEntity setIp(String ip) {
    setJsonData("ip", ip);

    FirmataBundleEntrypoint.UdpPayload udpPayload = FirmataBundleEntrypoint.getUdpFoundDevices().get(ip);
    if (udpPayload != null) {
      setTarget(udpPayload.getDeviceID());
      setBoardType(udpPayload.getBoard());
    }
    return this;
  }

  @Override
  @UIField(order = 100, hideInEdit = true)
  public String getIeeeAddress() {
    return super.getIeeeAddress();
  }

  @Override
  protected String getCommunicatorName() {
    return "ESP8266_WIFI";
  }

  @Override
  public FirmataDeviceCommunicator createFirmataDeviceType(EntityContext entityContext) {
    return new FirmataNetworkFirmataDeviceCommunicator(entityContext, this);
  }

  @Override
  protected boolean allowRegistrationType(PendingRegistrationContext pendingRegistrationContext) {
    return pendingRegistrationContext.getEntity() instanceof FirmataNetworkEntity;
  }

  @Override
  public String getEntityPrefix() {
    return PREFIX;
  }

  private static class FirmataNetworkFirmataDeviceCommunicator extends FirmataDeviceCommunicator<FirmataNetworkEntity> {

    public FirmataNetworkFirmataDeviceCommunicator(EntityContext entityContext, FirmataNetworkEntity entity) {
      super(entityContext, entity);
    }

    @Override
    protected IODevice createIODevice(FirmataNetworkEntity entity) {
      String ip = entity.getIp();
      return StringUtils.isEmpty(ip) ? null : new FirmataDevice(new NetworkTransport(ip + ":3132"));
    }

    @Override
    public long generateUniqueIDOnRegistrationSuccess() {
      return 1;
    }
  }

  public static class SelectFirmataIpDeviceLoader implements DynamicOptionLoader {

    @Override
    public List<OptionModel> loadOptions(DynamicOptionLoaderParameters parameters) {
      Map<String, FirmataBundleEntrypoint.UdpPayload> udpFoundDevices = FirmataBundleEntrypoint.getUdpFoundDevices();
      return udpFoundDevices.entrySet().stream().map(e -> OptionModel.of(e.getKey(), e.getValue().toString()))
          .collect(Collectors.toList());
    }
  }
}
