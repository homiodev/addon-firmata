package org.homio.addon.firmata.arduino.setting;

import org.homio.addon.firmata.arduino.ArduinoConsolePlugin;
import org.homio.api.Context;
import org.homio.api.console.ConsolePlugin;
import org.homio.api.model.Icon;
import org.homio.api.model.OptionModel;
import org.homio.api.setting.SettingPluginOptions;
import org.homio.api.setting.console.ConsoleSettingPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

public class ConsoleArduinoCompilerWarningSetting implements ConsoleSettingPlugin<String>,
  SettingPluginOptions<String> {

  @Override
  public @Nullable Icon getIcon() {
    return new Icon("fas fa-bug-slash");
  }

  @Override
  public @NotNull Collection<OptionModel> getOptions(Context context, JSONObject params) {
    return List.of(
      OptionModel.of("None"),
      OptionModel.of("Default"),
      OptionModel.of("More"),
      OptionModel.of("All"));
  }

  @Override
  public int order() {
    return 350;
  }

  @Override
  public @NotNull Class<String> getType() {
    return String.class;
  }

  @Override
  public boolean acceptConsolePluginPage(ConsolePlugin consolePlugin) {
    return consolePlugin instanceof ArduinoConsolePlugin;
  }

  @Override
  public @NotNull String getDefaultValue() {
    return "Default";
  }
}
