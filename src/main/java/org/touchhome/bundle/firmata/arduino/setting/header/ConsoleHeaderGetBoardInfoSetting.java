package org.touchhome.bundle.firmata.arduino.setting.header;

import org.json.JSONObject;
import org.touchhome.bundle.api.setting.SettingPluginButton;
import org.touchhome.bundle.api.setting.console.header.ConsoleHeaderSettingPlugin;

public class ConsoleHeaderGetBoardInfoSetting implements ConsoleHeaderSettingPlugin<JSONObject>, SettingPluginButton {

  @Override
  public String getIconColor() {
    return "#4279AE";
  }

  @Override
  public String getIcon() {
    return "fas fa-info";
  }

  @Override
  public String getConfirmMsg() {
    return null;
  }
}
