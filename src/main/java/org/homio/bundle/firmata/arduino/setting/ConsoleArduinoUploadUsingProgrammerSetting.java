package org.homio.bundle.firmata.arduino.setting;

import org.homio.bundle.api.console.ConsolePlugin;
import org.homio.bundle.api.setting.SettingPluginButton;
import org.homio.bundle.api.setting.console.ConsoleSettingPlugin;
import org.homio.bundle.firmata.arduino.ArduinoConsolePlugin;
import org.json.JSONObject;

public class ConsoleArduinoUploadUsingProgrammerSetting implements SettingPluginButton, ConsoleSettingPlugin<JSONObject> {

  @Override
  public String getIcon() {
    return "fas fa-upload";
  }

  @Override
  public int order() {
    return 300;
  }

  @Override
  public boolean acceptConsolePluginPage(ConsolePlugin consolePlugin) {
    return consolePlugin instanceof ArduinoConsolePlugin;
  }
}
