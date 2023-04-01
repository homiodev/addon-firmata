package org.homio.bundle.firmata.arduino;

import cc.arduino.Compiler;
import cc.arduino.UploaderUtils;
import cc.arduino.packages.BoardPort;
import cc.arduino.packages.Uploader;
import com.fazecast.jSerialComm.SerialPort;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.console.InlineLogsConsolePlugin;
import org.homio.bundle.api.exception.ServerException;
import org.homio.bundle.api.model.FileModel;
import org.homio.bundle.api.model.OptionModel;
import org.homio.bundle.api.setting.SettingPluginOptions;
import org.homio.bundle.api.setting.console.header.dynamic.DynamicConsoleHeaderSettingPlugin;
import org.homio.bundle.api.ui.field.ProgressBar;
import org.homio.bundle.api.ui.field.UIFieldType;
import org.homio.bundle.firmata.arduino.setting.ConsoleArduinoBoardPasswordSetting;
import org.homio.bundle.firmata.arduino.setting.ConsoleArduinoProgrammerSetting;
import org.homio.bundle.firmata.arduino.setting.header.ConsoleHeaderArduinoIncludeLibrarySetting;
import org.homio.bundle.firmata.arduino.setting.header.ConsoleHeaderArduinoPortSetting;
import org.homio.bundle.firmata.arduino.setting.header.ConsoleHeaderGetBoardsDynamicSetting;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import processing.app.BaseNoGui;
import processing.app.PreferencesData;
import processing.app.Sketch;
import processing.app.debug.RunnerException;
import processing.app.debug.TargetBoard;
import processing.app.debug.TargetPackage;
import processing.app.debug.TargetPlatform;
import processing.app.helpers.PreferencesMap;
import processing.app.helpers.PreferencesMapException;
import processing.app.packages.UserLibrary;

@Component
@RequiredArgsConstructor
public class ArduinoSketchService {

  private final EntityContext entityContext;
  private final InlineLogsConsolePlugin inlineLogsConsolePlugin;

  @Setter
  private Sketch sketch;

  public String build() {
    if (BaseNoGui.packages == null) {
      return null;
    }
    ProgressBar progressBar = (progress, message) -> entityContext.ui().progress("avr-build", progress, message, false);
    return inlineLogsConsolePlugin.consoleLogUsingStdout(() -> compileSketch(progressBar), progressBar::done);
  }

  public void upload(boolean usingProgrammer) {
    if (BaseNoGui.packages == null) {
      return;
    }
    ProgressBar progressBar = (progress, message) -> entityContext.ui().progress("avr-upload", progress, message, false);
    if (usingProgrammer) {
      PreferencesData.set("programmer", entityContext.setting().getValue(ConsoleArduinoProgrammerSetting.class));
    }

    UploaderUtils uploaderInstance = new UploaderUtils();
    Uploader uploader = uploaderInstance.getUploaderByPreferences(false);

    if (uploader.requiresAuthorization() && !PreferencesData.has(uploader.getAuthorizationKey())) {
      String boardPassword = entityContext.setting().getValue(ConsoleArduinoBoardPasswordSetting.class).optString("PASSWORD");
      PreferencesData.set(uploader.getAuthorizationKey(), boardPassword);
    }

    List<String> warningsAccumulator = new LinkedList<>();
    boolean success = false;
    try {
      if (!PreferencesData.has("serial.port")) {
        throw new ServerException("NO_PORT_SELECTED");
      }
      success = inlineLogsConsolePlugin.consoleLogUsingStdout(
          () -> {
            String fileName = compileSketch(progressBar);
            System.out.println("Uploading sketch...");
            progressBar.progress(20, "Uploading sketch...");
            boolean uploaded = uploaderInstance.upload(sketch, uploader, fileName, usingProgrammer, false, warningsAccumulator);
            if (uploaded) {
              System.out.println("Done uploading.");
            }
            return uploaded;
          },
          progressBar::done);
    } finally {
      if (uploader.requiresAuthorization() && !success) {
        PreferencesData.remove(uploader.getAuthorizationKey());
      }
    }
  }

  public void getBoardInfo() {
    SerialPort serialPort = entityContext.setting().getValue(ConsoleHeaderArduinoPortSetting.class);
    if (serialPort != null) {
      List<BoardPort> ports = BaseNoGui.getDiscoveryManager().discovery();
      BoardPort boardPort = ports.stream().filter(p -> p.getAddress().equals(serialPort.getSystemPortName())).findAny().orElse(null);
      if (boardPort != null) {
        entityContext.ui().sendJsonMessage("BOARD_INFO", boardPort);
      }
    } else {
      entityContext.ui().sendErrorMessage("NO_PORT_SELECTED");
    }
  }

  public void selectBoard(String board) {
    if (StringUtils.isNotEmpty(board)) {
      String[] values = board.split("~~~");
      TargetPackage targetPackage = BaseNoGui.packages.values().stream().filter(p -> p.getId().equals(values[0])).findAny()
          .orElseThrow(() -> new RuntimeException("NO_BOARD_SELECTED"));
      TargetPlatform targetPlatform = targetPackage.platforms().stream().filter(p -> p.getId().equals(values[1])).findAny()
          .orElseThrow(() -> new RuntimeException("NO_BOARD_SELECTED"));
      TargetBoard targetBoard = targetPlatform.getBoards().values().stream().filter(b -> b.getId().equals(values[2])).findAny()
          .orElseThrow(() -> new RuntimeException("NO_BOARD_SELECTED"));

      BaseNoGui.selectBoard(targetBoard);
      BaseNoGui.onBoardOrPortChange();

      List<DynamicConsoleHeaderSettingPlugin<?>> dynamicSettings = new ArrayList<>();
      if (!targetBoard.getMenuIds().isEmpty()) {
        for (Map.Entry<String, String> customMenuEntry : targetPlatform.getCustomMenus().entrySet()) {
          if (targetBoard.getMenuIds().contains(customMenuEntry.getKey())) {
            PreferencesMap preferencesMap = targetBoard.getMenuLabels(customMenuEntry.getKey());
            if (!preferencesMap.isEmpty()) {
              dynamicSettings.add(new BoardDynamicSettings(customMenuEntry.getKey(), customMenuEntry.getValue(), preferencesMap));
            }
          }
        }
      }
      entityContext.setting().reloadSettings(ConsoleHeaderGetBoardsDynamicSetting.class, dynamicSettings);
      entityContext.setting().reloadSettings(ConsoleArduinoProgrammerSetting.class);
      entityContext.setting().reloadSettings(ConsoleHeaderArduinoIncludeLibrarySetting.class);
    }
  }

  @SneakyThrows
  public void importLibrary(UserLibrary lib, FileModel content) {
    List<String> list = lib.getIncludes();
    if (list == null) {
      File srcFolder = lib.getSrcFolder();
      String[] headers = BaseNoGui.headerListFromIncludePath(srcFolder);
      list = Arrays.asList(headers);
    }
    if (list.isEmpty()) {
      return;
    }

    StringBuilder buffer = new StringBuilder();
    for (String aList : list) {
      buffer.append("#include <");
      buffer.append(aList);
      buffer.append(">\n");
    }
    buffer.append('\n');
    buffer.append(content.getContent());
    content.setContent(buffer.toString());
  }

  private String compileSketch(ProgressBar progressBar) throws RunnerException, PreferencesMapException, IOException {
    String compileMsg = "Compiling sketch...";
    progressBar.progress(20, compileMsg);
    System.out.println(compileMsg);
    String result;
    try {
      result = new Compiler(sketch).build(value -> progressBar.progress(value, compileMsg), true);
    } catch (Exception ex) {
      System.err.println("Error while compiling sketch: " + ex.getMessage());
      throw ex;
    }
    if (result != null) {
      System.out.println("Done compiling.");
    }
    return result;
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
    public String getIcon() {
      switch (key) {
        case "cpu":
          return "fas fa-microchip";
        case "baud":
          return "fas fa-tachometer-alt";
        case "xtal":
        case "CrystalFreq":
          return "fas fa-wave-square";
        case "eesz":
          return "fas fa-flask";
        case "ResetMethod":
          return "fas fa-trash-restore-alt";
        case "dbg":
          return "fab fa-hubspot";
        case "lvl":
          return "fas fa-level-up-alt";
        case "ip":
          return "fas fa-superscript";
        case "vt":
          return "fas fa-table";
        case "exception":
          return "fas fa-exclamation-circle";
        case "wipe":
          return "fas fa-eraser";
        case "ssl":
          return "fab fa-expeditedssl";
      }
      return "fas fa-wrench";
    }

    @Override
    public String getTitle() {
      return title;
    }

    @Override
    public Class<String> getType() {
      return String.class;
    }

    @Override
    public String getDefaultValue() {
      return preferencesMap.keySet().iterator().next();
    }

    @Override
    public UIFieldType getSettingType() {
      return UIFieldType.SelectBox;
    }

    @Override
    public Collection<OptionModel> getOptions(EntityContext entityContext, JSONObject params) {
      return OptionModel.list(preferencesMap);
    }

    @Override
    public int order() {
      return 0;
    }
  }
}
