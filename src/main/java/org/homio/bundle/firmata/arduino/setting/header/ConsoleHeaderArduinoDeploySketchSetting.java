package org.homio.bundle.firmata.arduino.setting.header;

import org.homio.bundle.api.setting.SettingPluginButton;
import org.homio.bundle.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.json.JSONObject;

public class ConsoleHeaderArduinoDeploySketchSetting implements ConsoleHeaderSettingPlugin<JSONObject>, SettingPluginButton {

  @Override
  public String getConfirmMsg() {
    return null;
  }

  @Override
  public String getIconColor() {
    return "#2F8B44";
  }

  @Override
  public String getIcon() {
    return "fas fa-upload";
  }

  @Override
  public String[] fireActionsBeforeChange() {
    return new String[]{"st_ShowInlineReadOnlyConsoleHeaderSetting/true", "SAVE"};
  }
}
