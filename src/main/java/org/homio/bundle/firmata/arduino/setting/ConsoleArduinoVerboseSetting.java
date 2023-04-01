package org.homio.bundle.firmata.arduino.setting;

import org.homio.bundle.api.console.ConsolePlugin;
import org.homio.bundle.api.setting.SettingPluginBoolean;
import org.homio.bundle.api.setting.console.ConsoleSettingPlugin;
import org.homio.bundle.firmata.arduino.ArduinoConsolePlugin;

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
