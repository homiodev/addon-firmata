package org.homio.bundle.firmata.arduino.setting;

import java.util.Collections;
import java.util.List;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.console.ConsolePlugin;
import org.homio.bundle.api.setting.SettingPluginButton;
import org.homio.bundle.api.setting.console.ConsoleSettingPlugin;
import org.homio.bundle.api.ui.field.action.ActionInputParameter;
import org.homio.bundle.api.ui.field.action.UIActionInput;
import org.homio.bundle.firmata.arduino.ArduinoConsolePlugin;
import org.json.JSONObject;

public class ConsoleArduinoBoardPasswordSetting implements SettingPluginButton, ConsoleSettingPlugin<JSONObject> {

  @Override
  public String getIcon() {
    return "fas fa-unlock-alt";
  }

  @Override
  public int order() {
    return 200;
  }

  @Override
  public boolean acceptConsolePluginPage(ConsolePlugin consolePlugin) {
    return consolePlugin instanceof ArduinoConsolePlugin;
  }

  @Override
  public List<ActionInputParameter> getInputParameters(EntityContext entityContext, String value) {
    return Collections.singletonList(new ActionInputParameter("PASSWORD", UIActionInput.Type.password, null, null)
        .setDescription("arduino.PASSWORD_DESCRIPTION"));
  }
}
