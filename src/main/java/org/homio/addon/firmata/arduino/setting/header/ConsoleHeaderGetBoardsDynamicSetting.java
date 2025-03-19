package org.homio.addon.firmata.arduino.setting.header;

import lombok.RequiredArgsConstructor;
import org.homio.api.Context;
import org.homio.api.model.Icon;
import org.homio.api.model.OptionModel;
import org.homio.api.setting.SettingPluginOptions;
import org.homio.api.setting.console.header.dynamic.DynamicConsoleHeaderContainerSettingPlugin;
import org.homio.api.setting.console.header.dynamic.DynamicConsoleHeaderSettingPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import processing.app.BaseNoGui;
import processing.app.PreferencesData;
import processing.app.debug.TargetBoard;
import processing.app.debug.TargetPlatform;
import processing.app.helpers.PreferencesMap;

import java.util.Collection;
import java.util.Map;

public class ConsoleHeaderGetBoardsDynamicSetting implements DynamicConsoleHeaderContainerSettingPlugin {

  private static void addSettings(@NotNull DynamicSettingConsumer consumer, TargetPlatform targetPlatform, TargetBoard targetBoard) {
    for (Map.Entry<String, String> customMenuEntry : targetPlatform.getCustomMenus().entrySet()) {
      if (targetBoard.getMenuIds().contains(customMenuEntry.getKey())) {
        PreferencesMap preferencesMap = targetBoard.getMenuLabels(customMenuEntry.getKey());
        if (!preferencesMap.isEmpty()) {
          var dynamicSetting = new BoardDynamicSettings(customMenuEntry.getKey(), customMenuEntry.getValue(), preferencesMap);
          consumer.addDynamicSetting(dynamicSetting);
          var firstKey = preferencesMap.keySet().iterator().next();
          var keyValue = targetBoard.getMenuPreferences(customMenuEntry.getKey(), firstKey);
          if (keyValue != null) {
            for (Map.Entry<String, String> entry : keyValue.entrySet()) {
              PreferencesData.set(entry.getKey(), entry.getValue());
            }
          }
        }
      }
    }
  }

  @Override
  public Icon getIcon() {
    return new Icon("fas fa-cubes", "#1C9CB0");
  }

  @Override
  public void assembleDynamicSettings(@NotNull Context context, @NotNull DynamicSettingConsumer consumer) {
    if (BaseNoGui.packages != null) {
      TargetBoard targetBoard = BaseNoGui.getTargetBoard();
      if (targetBoard != null) {
        TargetPlatform targetPlatform = targetBoard.getContainerPlatform();

        if (!targetBoard.getMenuIds().isEmpty()) {
          addSettings(consumer, targetPlatform, targetBoard);
        }
      }
    }
  }

  @RequiredArgsConstructor
  private static class BoardDynamicSettings implements DynamicConsoleHeaderSettingPlugin<String>, SettingPluginOptions<String> {

    private final String key;
    private final String title;
    private final PreferencesMap preferencesMap;

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public Icon getIcon() {
      return new Icon(switch (key) {
        case "cpu" -> "fas fa-microchip";
        case "baud" -> "fas fa-tachometer-alt";
        case "xtal", "CrystalFreq" -> "fas fa-wave-square";
        case "eesz" -> "fas fa-flask";
        case "ResetMethod" -> "fas fa-trash-restore-alt";
        case "dbg" -> "fab fa-hubspot";
        case "lvl" -> "fas fa-level-up-alt";
        case "ip" -> "fas fa-superscript";
        case "vt" -> "fas fa-table";
        case "exception" -> "fas fa-exclamation-circle";
        case "wipe" -> "fas fa-eraser";
        case "ssl" -> "fab fa-expeditedssl";
        default -> "fas fa-wrench";
      });
    }

    @Override
    public String getTitle() {
      return title;
    }

    @Override
    public @NotNull Class<String> getType() {
      return String.class;
    }

    @Override
    public @NotNull String getDefaultValue() {
      return preferencesMap.keySet().iterator().next();
    }

    @Override
    public @NotNull Collection<OptionModel> getOptions(Context context, JSONObject params) {
      return OptionModel.list(preferencesMap);
    }

    @Override
    public int order() {
      return 0;
    }
  }
}
