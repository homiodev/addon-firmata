package org.homio.bundle.firmata.arduino.setting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.console.ConsolePlugin;
import org.homio.bundle.api.model.OptionModel;
import org.homio.bundle.api.setting.SettingPluginOptions;
import org.homio.bundle.api.setting.console.ConsoleSettingPlugin;
import org.homio.bundle.api.ui.field.UIFieldType;
import org.homio.bundle.firmata.arduino.ArduinoConsolePlugin;
import org.json.JSONObject;
import processing.app.BaseNoGui;
import processing.app.debug.TargetBoard;
import processing.app.debug.TargetPlatform;
import processing.app.helpers.PreferencesMap;

public class ConsoleArduinoProgrammerSetting implements ConsoleSettingPlugin<String>,
    SettingPluginOptions<String> {

  @Override
  public Integer getMaxWidth() {
    return 80;
  }

  @Override
  public Collection<OptionModel> getOptions(EntityContext entityContext, JSONObject params) {
    List<OptionModel> programmerMenus = new ArrayList<>();

    if (BaseNoGui.packages != null) {
      TargetBoard board = BaseNoGui.getTargetBoard();
      if (board != null) {
        TargetPlatform boardPlatform = board.getContainerPlatform();
        TargetPlatform corePlatform = null;

        String core = board.getPreferences().get("build.core");
        if (core != null && core.contains(":")) {
          String[] split = core.split(":", 2);
          corePlatform = BaseNoGui.getCurrentTargetPlatformFromPackage(split[0]);
        }

        addProgrammersForPlatform(boardPlatform, programmerMenus);
        if (corePlatform != null) {
          addProgrammersForPlatform(corePlatform, programmerMenus);
        }
      }
    }
    return programmerMenus;
  }

  public void addProgrammersForPlatform(TargetPlatform platform, List<OptionModel> menus) {
    for (Map.Entry<String, PreferencesMap> entry : platform.getProgrammers().entrySet()) {
      String id = platform.getContainerPackage().getId() + ":" + entry.getKey();
      menus.add(OptionModel.of(id, entry.getValue().get("name")));
    }
  }

  @Override
  public int order() {
    return 250;
  }

  @Override
  public Class<String> getType() {
    return String.class;
  }

  @Override
  public UIFieldType getSettingType() {
    return UIFieldType.SelectBoxDynamic;
  }

  @Override
  public boolean acceptConsolePluginPage(ConsolePlugin consolePlugin) {
    return consolePlugin instanceof ArduinoConsolePlugin;
  }
}
