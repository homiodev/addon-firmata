package org.homio.addon.firmata.arduino;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.homio.addon.firmata.arduino.setting.ConsoleArduinoUploadUsingProgrammerSetting;
import org.homio.addon.firmata.arduino.setting.ConsoleArduinoVerboseSetting;
import org.homio.addon.firmata.arduino.setting.ConsoleArduinoVerifyAfterUploadSetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderArduinoBuildSketchSetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderArduinoDeploySketchSetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderArduinoGetBoardsSetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderArduinoIncludeLibrarySetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderArduinoPortSetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderArduinoSketchNameSetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderGetBoardInfoSetting;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderGetBoardsDynamicSetting;
import org.homio.api.Context;
import org.homio.api.console.ConsolePluginEditor;
import org.homio.api.exception.ServerException;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.model.FileContentType;
import org.homio.api.model.FileModel;
import org.homio.api.model.OptionModel;
import org.homio.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.homio.api.util.CommonUtils;
import org.homio.api.util.Lang;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import processing.app.BaseNoGui;
import processing.app.PreferencesData;
import processing.app.Sketch;
import processing.app.TextStorage;
import processing.app.packages.UserLibrary;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.homio.api.entity.HasJsonData.LEVEL_DELIMITER;

@Log4j2
@Component
@RequiredArgsConstructor
public class ArduinoConsolePlugin implements ConsolePluginEditor {

  public static final String DEFAULT_SKETCH_NAME = "sketch_default.ino";

  static {
    System.setProperty("APP_DIR", CommonUtils.getInstallPath().resolve("arduino").toString());
  }

  private final Context context;
  private final ArduinoSketchService arduinoSketchService;
  private Sketch sketch;
  private FileModel content = new FileModel(DEFAULT_SKETCH_NAME, "", FileContentType.cpp);
  private String prevContent = "";

  @SneakyThrows
  public void init() {
    this.open(this.content.getName(), true);

    context.setting().listenValueAndGet(ConsoleHeaderArduinoGetBoardsSetting.class, "avr-board",
      this.arduinoSketchService::selectBoard);

    context.setting().listenValue(ConsoleHeaderArduinoSketchNameSetting.class, "avr-file-name", path -> {
      try {
        this.open(path, false);
        this.syncContentToUI();
      } catch (Exception ex) {
        log.error(ex);
        context.ui().toastr().error("Error open file: " + path + ". Error: " + ex.getMessage() + ". Please try again later.");
      }
    });

    context.setting().listenValueAndGet(ConsoleArduinoVerboseSetting.class, "avr-verbose",
      value -> {
        PreferencesData.setBoolean("upload.verbose", value);
        PreferencesData.setBoolean("build.verbose", value);
      });

    context.setting().listenValueAndGet(ConsoleArduinoVerifyAfterUploadSetting.class, "avr-verify",
      value -> PreferencesData.setBoolean("upload.verify", value));

    context.setting().listenValue(ConsoleHeaderArduinoBuildSketchSetting.class, "avr-build",
      arduinoSketchService::build);

    context.setting().listenValue(ConsoleHeaderArduinoDeploySketchSetting.class, "avr-upload",
      () -> arduinoSketchService.upload(false));

    context.setting().listenValue(ConsoleArduinoUploadUsingProgrammerSetting.class, "avr-upload-using-programmer",
      () -> arduinoSketchService.upload(true));

    context.setting().listenValue(ConsoleHeaderGetBoardInfoSetting.class, "avr-get-board-info",
      arduinoSketchService::getBoardInfo);

    context.setting().listenValue(ConsoleHeaderArduinoPortSetting.class, "avr-select-port", serialPort -> {
      if (serialPort != null) {
        BaseNoGui.selectSerialPort(serialPort.getSystemPortName());
      } else {
        BaseNoGui.selectSerialPort("");
      }
      BaseNoGui.onBoardOrPortChange();
    });

    context.setting().listenValue(ConsoleHeaderArduinoIncludeLibrarySetting.class, "avr-include-lib", s -> {
      UserLibrary userLibrary = BaseNoGui.librariesIndexer.getInstalledLibraries().getByName(s);
      this.arduinoSketchService.importLibrary(userLibrary, this.content);
      this.syncContentToUI();
    });
  }

  @Override
  public Class<? extends ConsoleHeaderSettingPlugin<?>> getFileNameHeaderAction() {
    return ConsoleHeaderArduinoSketchNameSetting.class;
  }

  @Override
  public Map<String, Class<? extends ConsoleHeaderSettingPlugin<?>>> getHeaderActions() {
    Map<String, Class<? extends ConsoleHeaderSettingPlugin<?>>> headerActions = new LinkedHashMap<>();
    headerActions.put("verify", ConsoleHeaderArduinoBuildSketchSetting.class);
    headerActions.put("upload", ConsoleHeaderArduinoDeploySketchSetting.class);
    headerActions.put("getBoardInfo", ConsoleHeaderGetBoardInfoSetting.class);
    headerActions.put("arduinoPort", ConsoleHeaderArduinoPortSetting.class);
    headerActions.put("boards", ConsoleHeaderArduinoGetBoardsSetting.class);
    headerActions.put("dynamicBoardsInfo", ConsoleHeaderGetBoardsDynamicSetting.class);
    headerActions.put("incl_library", ConsoleHeaderArduinoIncludeLibrarySetting.class);
    return headerActions;
  }

  @SneakyThrows
  @Override
  public ActionResponseModel save(FileModel fileModel) {
    if (BaseNoGui.packages == null) {
      return ActionResponseModel.showWarn("REQUIRE_UPDATES");
    }
    if (fileModel.getName() == null) {
      fileModel.setName(this.content.getName());
    }
    if (!fileModel.getName().endsWith(".ino")) {
      fileModel.setName(fileModel.getName() + ".ino");
    }
    this.content = fileModel;
    if(!this.content.equals(fileModel)) {
      this.updateSketch();
      return ActionResponseModel.showSuccess(Lang.getServerMessage("SKETCH_SAVED"));
    }
    return null;
  }

  public void updateSketch() throws IOException {
    String properParent = content.getName().substring(0, content.getName().length() - 4);
    if (this.sketch == null || !this.sketch.getName().equals(properParent)) {
      Path sketchFile = BaseNoGui.getSketchbookFolder().toPath().resolve(properParent).resolve(content.getName());
      Files.createDirectories(sketchFile.getParent());
      if (!Files.exists(sketchFile)) {
        Files.createFile(sketchFile);
        // update ui part
        context.setting().reloadSettings(ConsoleHeaderArduinoSketchNameSetting.class);
        this.prevContent = ""; // uses for save content;
      }
      this.sketch = new Sketch(sketchFile.toFile());
      this.sketch.getPrimaryFile().setStorage(new TextStorage() {
        @Override
        public String getText() {
          return content.getContent();
        }

        @Override
        public boolean isModified() {
          return !prevContent.equals(content.getContent());
        }

        @Override
        public void clearModified() {
          prevContent = content.getContent();
        }
      });
      this.arduinoSketchService.setSketch(this.sketch);
    }
    // somehow file was removed
    if (!sketch.getPrimaryFile().getFile().exists()) {
      sketch.save();
      context.setting().reloadSettings(ConsoleHeaderArduinoSketchNameSetting.class);
    } else {
      sketch.save();
    }
  }

  @SneakyThrows
  @Override
  public ActionResponseModel glyphClicked(String line) {
    Pattern pattern = Pattern.compile("#include( +)[<\"](.*)\\.h[>\"]");
    Matcher matcher = pattern.matcher(line);
    Set<FileModel> files = new HashSet<>();
    if (matcher.find()) {
      String includeSource = matcher.group(2);
      for (UserLibrary library : BaseNoGui.librariesIndexer.getInstalledLibraries()) {
        Path hFile = library.getSrcFolder().toPath().resolve(includeSource + ".h");
        if (Files.exists(hFile)) {
          files.add(new FileModel(includeSource + ".h", new String(Files.readAllBytes(hFile)), FileContentType.cpp));

          Path cppFile = library.getSrcFolder().toPath().resolve(includeSource + ".cpp");
          if (Files.exists(cppFile)) {
            files.add(new FileModel(includeSource + ".cpp", new String(Files.readAllBytes(cppFile)),
              FileContentType.cpp));
          }
        }
      }
    }
    if (files.isEmpty()) {
      return null;
    }
    return ActionResponseModel.showFiles(files);
  }

  @Override
  public MonacoGlyphAction getGlyphAction() {
    return new MonacoGlyphAction("fas fa-external-link-square-alt", null, "#include( +)[<\"]\\w*\\.h[\">]");
  }

  public void syncContentToUI() {
    this.sendValueToConsoleEditor(context);
  }

  @SneakyThrows
  private void open(String fileName, boolean createIfNotExists) {
    if (BaseNoGui.getSketchbookFolder() == null) {
      return;
    }
    Path sketchFile;
    if (fileName.contains(LEVEL_DELIMITER)) { // contains example path. Read full content and copy to example file
      OptionModel foundModel = Optional.ofNullable(ConsoleHeaderArduinoSketchNameSetting.buildExamplePath(true))
        .map(e -> e.findByKey(fileName)).orElse(null);
      if (foundModel != null) {
        Path path = Paths.get(foundModel.getJson().get("path").asText());
        save(new FileModel(path.getFileName().toString(), new String(Files.readAllBytes(path)), FileContentType.cpp));
      }
      return;
    } else {
      if (!fileName.endsWith(".ino")) {
        throw new ServerException("Arduino file " + fileName + " must ends with .ino");
      }
      String properParent = fileName.substring(0, fileName.length() - 4);
      sketchFile = BaseNoGui.getSketchbookFolder().toPath().resolve(properParent).resolve(fileName);
    }

    if (!Files.exists(sketchFile)) {
      if (createIfNotExists) {
        Files.createDirectories(sketchFile.getParent());
        var stream = new ByteArrayInputStream(defaultSketch.getBytes());
        Files.copy(stream, sketchFile);
      } else {
        throw new ServerException("Unable to find file: " + sketchFile);
      }
    }

    this.content.setName(fileName);
    this.content.setContent(new String(Files.readAllBytes(sketchFile)));
    this.prevContent = this.content.getContent();
    this.updateSketch();
  }

  @Override
  public FileContentType getContentType() {
    return FileContentType.cpp;
  }

  @Override
  public String accept() {
    return ".ino, .cpp";
  }

  @Override
  public @NotNull Context context() {
    return context;
  }

  @Override
  public FileModel getValue() {
    return content;
  }

  private static final String defaultSketch = """
    void setup() {
      // put your setup code here, to run once:
    
    }
    
    void loop() {
      // put your main code here, to run repeatedly:
    
    }
    """;
}
