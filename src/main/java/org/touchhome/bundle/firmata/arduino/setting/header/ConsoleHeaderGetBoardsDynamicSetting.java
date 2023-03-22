package org.touchhome.bundle.firmata.arduino.setting.header;

import org.touchhome.bundle.api.setting.console.header.dynamic.DynamicConsoleHeaderContainerSettingPlugin;

public class ConsoleHeaderGetBoardsDynamicSetting implements DynamicConsoleHeaderContainerSettingPlugin {

  @Override
  public String getIcon() {
    return "fas fa-cubes";
  }

  @Override
  public String getIconColor() {
    return "#1C9CB0";
  }
}
