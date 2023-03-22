package org.touchhome.bundle.firmata.arduino.setting;

import org.json.JSONObject;
import org.touchhome.bundle.api.console.ConsolePlugin;
import org.touchhome.bundle.api.setting.SettingPluginButton;
import org.touchhome.bundle.api.setting.console.ConsoleSettingPlugin;
import org.touchhome.bundle.firmata.arduino.ArduinoConsolePlugin;

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
