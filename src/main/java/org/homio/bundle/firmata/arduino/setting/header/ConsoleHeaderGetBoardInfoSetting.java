package org.homio.bundle.firmata.arduino.setting.header;

import org.homio.bundle.api.setting.SettingPluginButton;
import org.homio.bundle.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.json.JSONObject;

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
