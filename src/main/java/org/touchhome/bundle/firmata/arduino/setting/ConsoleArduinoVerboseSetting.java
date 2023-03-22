package org.touchhome.bundle.firmata.arduino.setting;

import org.touchhome.bundle.api.console.ConsolePlugin;
import org.touchhome.bundle.api.setting.SettingPluginBoolean;
import org.touchhome.bundle.api.setting.console.ConsoleSettingPlugin;
import org.touchhome.bundle.firmata.arduino.ArduinoConsolePlugin;

public class ConsoleArduinoVerboseSetting implements SettingPluginBoolean, ConsoleSettingPlugin<Boolean> {

  @Override
  public int order() {
    return 500;
  }

  @Override
  public boolean acceptConsolePluginPage(ConsolePlugin consolePlugin) {
    return consolePlugin instanceof ArduinoConsolePlugin;
  }
}
