package org.homio.bundle.firmata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.EntityContextSetting;
import org.homio.bundle.api.entity.types.MicroControllerBaseEntity;
import org.homio.bundle.api.model.ActionResponseModel;
import org.homio.bundle.api.model.FileModel;
import org.homio.bundle.api.model.OptionModel;
import org.homio.bundle.api.model.Status;
import org.homio.bundle.api.ui.action.DynamicOptionLoader;
import org.homio.bundle.api.ui.field.UIField;
import org.homio.bundle.api.ui.field.action.UIContextMenuAction;
import org.homio.bundle.api.ui.field.color.UIFieldColorStatusMatch;
import org.homio.bundle.api.ui.field.selection.UIFieldSelectValueOnEmpty;
import org.homio.bundle.api.ui.field.selection.UIFieldSelection;
import org.homio.bundle.api.util.CommonUtils;
import org.homio.bundle.firmata.arduino.ArduinoConsolePlugin;
import org.homio.bundle.firmata.provider.FirmataDeviceCommunicator;
import org.homio.bundle.firmata.provider.IODeviceWrapper;
import org.homio.bundle.firmata.provider.command.FirmataRegisterCommand;
import org.homio.bundle.firmata.provider.command.PendingRegistrationContext;
import org.jetbrains.annotations.NotNull;


@Entity
@Accessors(chain = true)
public abstract class FirmataBaseEntity<T extends FirmataBaseEntity<T>> extends MicroControllerBaseEntity<T> {

  private static final Map<String, FirmataDeviceCommunicator> entityIDToDeviceCommunicator = new HashMap<>();

  @Setter
  @Getter
  @Transient
  @JsonIgnore
  private FirmataDeviceCommunicator firmataDeviceCommunicator;

  @UIField(order = 11, hideInEdit = true)
  @UIFieldColorStatusMatch
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public Status getJoined() {
    return EntityContextSetting.getStatus(this, "joined", Status.UNKNOWN);
  }

  public T setJoined(@NotNull Status status) {
    EntityContextSetting.setStatus(this, "joined", "Joined", status);
    return (T) this;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @UIField(order = 23, hideInEdit = true)
  public String getBoardType() {
    return getJsonData("boardType");
  }

  public void setBoardType(String boardType) {
    setJsonData("boardType", boardType);
  }

  @Override
  @UIField(order = 100, inlineEdit = true)
  @UIFieldSelection(SelectTargetFirmataDeviceLoader.class)
  @UIFieldSelectValueOnEmpty(label = "selection.target", color = "#A7D21E")
  public String getIeeeAddress() {
    return super.getIeeeAddress();
  }

  @UIContextMenuAction("RESTART")
  public ActionResponseModel restartCommunicator() {
    if (firmataDeviceCommunicator != null) {
      try {
        String response = firmataDeviceCommunicator.restart();
        return ActionResponseModel.showSuccess(response);
      } catch (Exception ex) {
        return ActionResponseModel.showError(ex);
      }
    }
    return ActionResponseModel.showWarn("action.communicator.not_found");
  }

  @UIContextMenuAction("UPLOAD_SKETCH")
  public void uploadSketch(EntityContext entityContext) {

  }

  @UIContextMenuAction("UPLOAD_SKETCH_MANUALLY")
  public void uploadSketchManually(EntityContext entityContext) {
    ArduinoConsolePlugin arduinoConsolePlugin = entityContext.getBean(ArduinoConsolePlugin.class);
    String content = CommonUtils.getResourceAsString("firmata", "arduino_firmata.ino");
    String commName = this.getCommunicatorName();
    String sketch = "#define COMM_" + commName + "\n" + content;
    arduinoConsolePlugin.save(new FileModel("arduino_firmata_" + commName + ".ino", sketch, null, false));
    arduinoConsolePlugin.syncContentToUI();
    entityContext.ui().openConsole(arduinoConsolePlugin);
  }

  protected abstract String getCommunicatorName();

  @JsonIgnore
  public short getTarget() {
    return getIeeeAddress() == null ? -1 : Short.parseShort(getIeeeAddress());
  }

  public void setTarget(short target) {
    setIeeeAddress(Short.toString(target));
  }

  @Override
  public String getDefaultName() {
    return "Firmata";
  }

  @Override
  public int getOrder() {
    return 20;
  }

  public abstract FirmataDeviceCommunicator createFirmataDeviceType(EntityContext entityContext);

  @JsonIgnore
  public final IODeviceWrapper getDevice() {
    return firmataDeviceCommunicator.getDevice();
  }

  public long getUniqueID() {
    return getJsonData("uniqueID", 0L);
  }

  public FirmataBaseEntity<T> setUniqueID(Long uniqueID) {
    setJsonData("uniqueID", uniqueID);
    return this;
  }

  protected abstract boolean allowRegistrationType(PendingRegistrationContext pendingRegistrationContext);

  @Override
  public void afterFetch(EntityContext entityContext) {
    setFirmataDeviceCommunicator(entityIDToDeviceCommunicator.computeIfAbsent(getEntityID(),
        ignore -> createFirmataDeviceType(entityContext)));
  }

  @Override
  public void beforeDelete(EntityContext entityContext) {
    FirmataDeviceCommunicator firmataDeviceCommunicator = entityIDToDeviceCommunicator.remove(getEntityID());
    if (firmataDeviceCommunicator != null) {
      firmataDeviceCommunicator.destroy();
    }
  }

  public static class SelectTargetFirmataDeviceLoader implements DynamicOptionLoader {

    @Override
    public List<OptionModel> loadOptions(DynamicOptionLoaderParameters parameters) {
      return parameters.getEntityContext().getBean(FirmataRegisterCommand.class).getPendingRegistrations().entrySet().stream()
          .filter(entry -> ((FirmataBaseEntity) parameters.getBaseEntity()).allowRegistrationType(entry.getValue()))
          .map(entry -> OptionModel.of(Short.toString(entry.getKey()), entry.getKey() + "/" + entry.getValue()))
          .collect(Collectors.toList());
    }
  }
}
