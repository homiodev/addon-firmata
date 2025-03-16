package org.homio.addon.firmata.arduino.setting.header;

import org.homio.api.Context;
import org.homio.api.model.Icon;
import org.homio.api.setting.console.header.dynamic.DynamicConsoleHeaderContainerSettingPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class ConsoleHeaderGetBoardsDynamicSetting implements DynamicConsoleHeaderContainerSettingPlugin {

  @Override
  public Icon getIcon() {
    return new Icon("fas fa-cubes", "#1C9CB0");
  }

  @Override
  public @NotNull JSONObject getParameters(Context context, String value) {
    return DynamicConsoleHeaderContainerSettingPlugin.super.getParameters(context, value);
  }
}
