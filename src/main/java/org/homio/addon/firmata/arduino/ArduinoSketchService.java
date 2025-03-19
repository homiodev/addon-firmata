package org.homio.addon.firmata.arduino;

import cc.arduino.Compiler;
import cc.arduino.UploaderUtils;
import cc.arduino.packages.BoardPort;
import cc.arduino.packages.Uploader;
import com.fazecast.jSerialComm.SerialPort;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.homio.addon.firmata.arduino.setting.ConsoleArduinoBoardPasswordSetting;
import org.homio.addon.firmata.arduino.setting.ConsoleArduinoProgrammerSetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderArduinoIncludeLibrarySetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderArduinoPortSetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderGetBoardsDynamicSetting;
import org.homio.api.Context;
import org.homio.api.exception.ServerException;
import org.homio.api.model.FileModel;
import org.homio.api.util.Lang;
import org.homio.hquery.ProgressBar;
import org.springframework.stereotype.Component;
import processing.app.BaseNoGui;
import processing.app.PreferencesData;
import processing.app.Sketch;
import processing.app.debug.RunnerException;
import processing.app.debug.TargetBoard;
import processing.app.debug.TargetPackage;
import processing.app.debug.TargetPlatform;
import processing.app.helpers.PreferencesMapException;
import processing.app.packages.UserLibrary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.homio.api.entity.HasJsonData.LEVEL_DELIMITER;

@Component
@RequiredArgsConstructor
public class ArduinoSketchService {

  public static final String PACKAGE_PLATFORM_DELIMITER = "@@@";
  private final Context context;

  @Setter
  private Sketch sketch;

  public String build() {
    if (BaseNoGui.packages == null) {
      return null;
    }
    ProgressBar progressBar = (progress, message, error) ->
      context.ui().progress().update("avr-build", progress, message, false);
    return context.ui().console().streamInlineConsole("Build sketch",
      () -> compileSketch(progressBar), progressBar::done);
  }

  public void upload(boolean usingProgrammer) {
    if (BaseNoGui.packages == null) {
      return;
    }
    ProgressBar progressBar = (progress, message, error) ->
      context.ui().progress().update("avr-upload", progress, message, false);
    if (usingProgrammer) {
      PreferencesData.set("programmer", context.setting().getValue(ConsoleArduinoProgrammerSetting.class));
    }

    UploaderUtils uploaderInstance = new UploaderUtils();
    Uploader uploader = uploaderInstance.getUploaderByPreferences(false);

    if (uploader.requiresAuthorization() && !PreferencesData.has(uploader.getAuthorizationKey())) {
      String boardPassword = context.setting().getValue(ConsoleArduinoBoardPasswordSetting.class).optString("PASSWORD");
      PreferencesData.set(uploader.getAuthorizationKey(), boardPassword);
    }

    List<String> warningsAccumulator = new LinkedList<>();
    boolean success = false;
    try {
      if (!PreferencesData.has("serial.port")) {
        throw new ServerException("W.ERROR.NO_PORT_SELECTED");
      }
      success = context.ui().console().streamInlineConsole("Deploy sketch",
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
    SerialPort serialPort = context.setting().getValue(ConsoleHeaderArduinoPortSetting.class);
    if (serialPort != null) {
      List<BoardPort> ports = BaseNoGui.getDiscoveryManager().discovery();
      BoardPort boardPort = ports.stream().filter(p -> p.getAddress().equals(serialPort.getSystemPortName())).findAny().orElse(null);
      if (boardPort != null) {
        context.ui().sendJsonMessage("BOARD_INFO", boardPort);
      } else {
        context.ui().toastr().warn(Lang.getServerMessage("NO_BOARD_INFO"));
      }
    } else {
      context.ui().toastr().error("W.ERROR.NO_PORT_SELECTED");
    }
  }

  public void selectBoard(String board) {
    if (StringUtils.isNotEmpty(board)) {
      String[] values = board.split(LEVEL_DELIMITER);
      String[] packageAndPlatform = values[0].split(PACKAGE_PLATFORM_DELIMITER);
      String packageName = packageAndPlatform[0];
      String platformName = packageAndPlatform[1];
      String boardName = values[1];
      TargetPackage targetPackage = BaseNoGui.packages.values().stream().filter(p -> p.getId().equals(packageName)).findAny()
        .orElseThrow(() -> new RuntimeException("NO_BOARD_SELECTED"));
      TargetPlatform targetPlatform = targetPackage.platforms().stream().filter(p -> p.getId().equals(platformName)).findAny()
        .orElseThrow(() -> new RuntimeException("NO_BOARD_SELECTED"));
      TargetBoard targetBoard = targetPlatform.getBoards().values().stream().filter(b -> b.getId().equals(boardName)).findAny()
        .orElseThrow(() -> new RuntimeException("NO_BOARD_SELECTED"));

      BaseNoGui.selectBoard(targetBoard);
      BaseNoGui.onBoardOrPortChange();

      context.setting().reloadDynamicSettings(ConsoleHeaderGetBoardsDynamicSetting.class);
      context.setting().reloadSettings(ConsoleArduinoProgrammerSetting.class);
      context.setting().reloadSettings(ConsoleHeaderArduinoIncludeLibrarySetting.class);
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
}
