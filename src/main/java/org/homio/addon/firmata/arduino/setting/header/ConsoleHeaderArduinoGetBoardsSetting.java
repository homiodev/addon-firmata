package org.homio.addon.firmata.arduino.setting.header;

import org.homio.api.Context;
import org.homio.api.model.Icon;
import org.homio.api.model.OptionModel;
import org.homio.api.setting.SettingPluginOptions;
import org.homio.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import processing.app.BaseNoGui;
import processing.app.debug.TargetBoard;
import processing.app.debug.TargetPackage;
import processing.app.debug.TargetPlatform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.homio.addon.firmata.arduino.ArduinoSketchService.PACKAGE_PLATFORM_DELIMITER;

public class ConsoleHeaderArduinoGetBoardsSetting implements ConsoleHeaderSettingPlugin<String>,
  SettingPluginOptions<String> {

  @Override
  public boolean lazyLoad() {
    return true;
  }

  @Override
  public @NotNull Collection<OptionModel> getOptions(Context context, JSONObject params) {
    List<OptionModel> options = new ArrayList<>();
    // Cycle through all packages
    if (BaseNoGui.packages != null) {
      for (TargetPackage targetPackage : BaseNoGui.packages.values()) {
        // For every package cycle through all platform
        for (TargetPlatform targetPlatform : targetPackage.platforms()) {

          // Add a title for each platform
          String platformLabel = targetPlatform.getPreferences().get("name");
          if (platformLabel == null) {
            platformLabel = targetPackage.getId() + "-" + targetPlatform.getId();
          }

          // add an hint that this core lives in sketchbook
          if (targetPlatform.isInSketchbook()) {
            platformLabel += " (in sketchbook)";
          }

          OptionModel boardFamily = OptionModel.of(targetPackage.getId() + PACKAGE_PLATFORM_DELIMITER + targetPlatform.getId(), platformLabel);

          for (TargetBoard board : targetPlatform.getBoards().values()) {
            if (board.getPreferences().get("hide") != null) {
              continue;
            }
            OptionModel boardType = OptionModel.of(board.getId(), board.getName());
            boardFamily.addChild(boardType);
          }
          if (boardFamily.hasChildren()) {
            options.add(boardFamily);
          }
        }
      }
    }
    return options;
  }

  @Override
  public @NotNull Class<String> getType() {
    return String.class;
  }

  @Override
  public Integer getMaxWidth() {
    return 150;
  }

  @Override
  public Icon getIcon() {
    return new Icon("fas fa-microchip");
  }

  @Override
  public boolean viewAsButton() {
    return true;
  }

  @Override
  public boolean isRequired() {
    return true;
  }
}
