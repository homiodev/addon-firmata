package org.touchhome.bundle.firmata.arduino.setting.header;

import org.json.JSONObject;
import org.touchhome.bundle.api.setting.SettingPluginButton;
import org.touchhome.bundle.api.setting.console.header.ConsoleHeaderSettingPlugin;

public class ConsoleHeaderArduinoBuildSketchSetting implements ConsoleHeaderSettingPlugin<JSONObject>, SettingPluginButton {

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
    return "fas fa-check";
  }

  @Override
  public String[] fireActionsBeforeChange() {
    return new String[]{"st_ShowInlineReadOnlyConsoleHeaderSetting/true", "SAVE"};
  }
}
