package org.homio.addon.firmata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import lombok.experimental.Accessors;
import org.homio.addon.firmata.FirmataEntrypoint;
import org.homio.addon.firmata.arduino.ArduinoConsolePlugin;
import org.homio.api.Context;
import org.homio.api.entity.HasPlace;
import org.homio.api.entity.device.DeviceEndpointsBehaviourContract;
import org.homio.api.entity.device.HasExcludeEndpoints;
import org.homio.api.entity.log.HasEntityLog;
import org.homio.api.entity.types.MicroControllerBaseEntity;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.model.FileContentType;
import org.homio.api.model.FileModel;
import org.homio.api.model.device.ConfigDeviceDefinition;
import org.homio.api.model.endpoint.DeviceEndpoint;
import org.homio.api.service.EntityService;
import org.homio.api.ui.UI;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.UIFieldGroup;
import org.homio.api.ui.field.UIFieldInlineEditConfirm;
import org.homio.api.ui.field.UIFieldNoReadDefaultValue;
import org.homio.api.ui.field.UIFieldSlider;
import org.homio.api.ui.field.UIFieldType;
import org.homio.api.ui.field.action.UIContextMenuAction;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.homio.api.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;


@SuppressWarnings({"unused", "UnusedReturnValue", "JpaAttributeTypeInspection"})
@Entity
@Accessors(chain = true)
public abstract class FirmataBaseEntity<T extends FirmataBaseEntity<T>>
  extends MicroControllerBaseEntity
  implements DeviceEndpointsBehaviourContract,
  EntityService<FirmataService>,
  HasEntityLog,
  HasExcludeEndpoints,
  HasPlace {

  @Override
  public void logBuilder(@NotNull HasEntityLog.EntityLogBuilder entityLogBuilder) {
    entityLogBuilder.addTopicFilterByEntityID(FirmataEntrypoint.class);
  }


  @Override
  public void assembleActions(UIInputBuilder uiInputBuilder) {

  }

  @Override
  public @NotNull List<ConfigDeviceDefinition> findMatchDeviceConfigurations() {
    return List.of();
  }

  @Override
  public @NotNull String getDeviceFullName() {
    return getTitle();
  }

  @Override
  public @NotNull Map<String, ? extends DeviceEndpoint> getDeviceEndpoints() {
    return getService().getEndpoints();
  }

  @UIField(order = 1, inlineEdit = true)
  @UIFieldInlineEditConfirm(value = "W.CONFIRM.TOGGLE_SERVICE", dialogColor = UI.Color.RED)
  @UIFieldGroup("GENERAL")
  public boolean isStart() {
    return getJsonData("start", true);
  }

  public void setStart(boolean start) {
    setJsonData("start", start);
  }

  @UIField(order = 23, disableEdit = true)
  @UIFieldNoReadDefaultValue
  public String getBoard() {
    return getJsonData("bn");
  }

  public FirmataBaseEntity<T> setBoard(String value) {
    setJsonData("bn", value);
    return this;
  }

  @UIField(order = 25)
  @UIFieldSlider(min = 5, max = 60, header = "min")
  @UIFieldGroup("CONNECT")
  public int getWatchDogTimeout() {
    return getJsonData("wdt", 10);
  }

  public void setWatchDogTimeout(int value) {
    setJsonData("wdt", value);
  }

  @UIFieldGroup(value = "TIMES", borderColor = "#871DB8")
  @UIField(order = 26, hideInEdit = true, type = UIFieldType.Duration, hideOnEmpty = true)
  public Long getDeviceWorkingTime() {
    return getService().getDeviceStartedTime();
  }

  @UIFieldGroup("TIMES")
  @UIField(order = 27, hideInEdit = true, type = UIFieldType.Duration, hideOnEmpty = true)
  public Long getDeviceLastPingResponseTime() {
    return getService().getLastPingResponseTime();
  }

  @UIFieldGroup("TIMES")
  @UIField(order = 28, hideInEdit = true, type = UIFieldType.Duration, hideOnEmpty = true)
  public Long getLastInitResponseTime() {
    return getService().getLastInitResponseTime();
  }

  @UIFieldGroup("TIMES")
  @UIField(order = 29, hideInEdit = true, type = UIFieldType.DurationDowntime, hideOnEmpty = true)
  public Long getWatchDogTimeoutToRestart() {
    return getService().getWatchDogTimeoutToRestart();
  }

  @UIField(order = 35)
  @UIFieldSlider(min = -1, max = 10, header = "min")
  @UIFieldGroup("CONNECT")
  public int getPingInterval() {
    return getJsonData("pi", 1);
  }

  public void setPingInterval(int value) {
    setJsonData("pi", value);
  }

  @Override
  @UIField(order = 100, disableEdit = true)
  @UIFieldGroup("GENERAL")
  @UIFieldNoReadDefaultValue
  public String getIeeeAddress() {
    return super.getIeeeAddress();
  }

  @UIContextMenuAction(value = "RESTART_COMMUNICATOR", icon = "fas fa-power-off", iconColor = UI.Color.RED)
  public ActionResponseModel restartCommunicator() {
    String result = getService().restart();
    if (result.endsWith("_error")) {
      return ActionResponseModel.showError(result);
    }
    return ActionResponseModel.showSuccess(result);
  }

  @UIContextMenuAction(value = "UPLOAD_SKETCH_MANUALLY", icon = "fas fa-upload")
  public void uploadSketchManually(Context context) {
    ArduinoConsolePlugin arduinoConsolePlugin = context.getBean(ArduinoConsolePlugin.class);
    String content = CommonUtils.getResourceAsString("firmata", "arduino_firmata.ino");
    String commName = this.getCommunicatorName();
    String sketch = "#define ENABLE_" + commName + "\n" + content;
    arduinoConsolePlugin.save(new FileModel("arduino_firmata_" + commName + ".ino", sketch, FileContentType.cpp));
    arduinoConsolePlugin.syncContentToUI();
    context.ui().console().openConsole(arduinoConsolePlugin.getName());
  }

  protected abstract String getCommunicatorName();

  @JsonIgnore
  public short getDeviceID() {
    return getIeeeAddress() == null ? -1 : Short.parseShort(getIeeeAddress());
  }

  public void setDeviceID(short target) {
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

  @Override
  public @Nullable FirmataService createService(@NotNull Context context) {
    return new FirmataService(context, this, true);
  }

  @Override
  public @NotNull Class<FirmataService> getEntityServiceItemClass() {
    return FirmataService.class;
  }

  public boolean tryUpdateEntity(FirmataService.DeviceInfo info) {
    return tryUpdateEntity(() -> {
      setBoard(info.board());
      setIeeeAddress(info.deviceID());
      setName(info.name());
    });
  }

  @Override
  public void beforePersist() {
    super.beforePersist();
    setExcludeEndpoints(String.join(LIST_DELIMITER, Set.of("TX", "RX")));
  }
}
