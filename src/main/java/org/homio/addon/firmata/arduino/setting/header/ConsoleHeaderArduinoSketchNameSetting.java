package org.homio.addon.firmata.arduino.setting.header;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.homio.api.Context;
import org.homio.api.model.OptionModel;
import org.homio.api.setting.SettingPluginOptions;
import org.homio.api.setting.SettingPluginOptionsRemovable;
import org.homio.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import processing.app.BaseNoGui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.homio.addon.firmata.arduino.ArduinoConsolePlugin.DEFAULT_SKETCH_NAME;

@Log4j2
public class ConsoleHeaderArduinoSketchNameSetting implements
  SettingPluginOptions<String>,
  SettingPluginOptionsRemovable<String>,
  ConsoleHeaderSettingPlugin<String> {

  public static OptionModel buildExamplePath(boolean includePath) {
    OptionModel examples = OptionModel.key("examples");
    addSketches(examples, BaseNoGui.getExamplesFolder(), includePath);
    if (examples.hasChildren()) {
      return examples;
    }
    return null;
  }

  private static void addSketches(OptionModel menu, File folder, boolean includePath) {
    if (folder != null && folder.isDirectory()) {
      File[] files = folder.listFiles();
      // If a bad folder or unreadable or whatever, this will come back null
      if (files != null) {
        // Alphabetize files, since it's not always alpha order
        Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        for (File subfolder : files) {
          if (!processing.app.helpers.FileUtils.isSCCSOrHiddenFile(subfolder) && subfolder.isDirectory()) {
            addSketchesSubmenu(menu, subfolder.getName(), subfolder, includePath);
          }
        }
      }
    }
  }

  private static void addSketchesSubmenu(OptionModel menu, String name, File folder, boolean includePath) {
    File entry = new File(folder, name + ".ino");
    if (entry.exists()) {
      if (BaseNoGui.isSanitaryName(name)) {
        OptionModel optionModel = OptionModel.of(name, name + ".ino");
        menu.addChild(optionModel);
        if (includePath) {
          optionModel.json(jsonObject -> jsonObject.put("path", entry.getAbsolutePath()));
        }
      }
      return;
    }

    // don't create an extra menu level for a folder named "examples"
    if (folder.getName().equals("examples")) {
      addSketches(menu, folder, includePath);
    } else {
      // not a sketch folder, but maybe a subfolder containing sketches
      OptionModel submenu = OptionModel.of(name, name);
      addSketches(submenu, folder, includePath);
      menu.addChildIfHasSubChildren(submenu);
    }
  }

  @Override
  public @NotNull String getDefaultValue() {
    return DEFAULT_SKETCH_NAME;
  }

 /* @Override
  public String getIcon() {
    return SettingPluginOptionsFileExplorer.super.getIcon();
  }

  @Override
  public Path rootPath() {
    return BaseNoGui.packages == null ? null : BaseNoGui.getSketchbookFolder().toPath();
  }

  @Override
  public int levels() {
    return 2;
  }*/

  /*@Override
  public boolean writeDirectory(Path dir) {
    return false;
  }

  @Override
  public boolean flatStructure() {
    return true;
  }

  @Override
  public boolean writeFile(Path path, BasicFileAttributes attrs) {
    return path.getFileName().toString().endsWith(".ino");
  }*/

  @SneakyThrows
  @Override
  public @NotNull Collection<OptionModel> getOptions(Context context, JSONObject params) {
    var options = new ArrayList<OptionModel>();
    OptionModel examples = buildExamplePath(false);
    if (examples != null) {
      options.add(OptionModel.separator());
      options.add(examples);
    }
    options.add(OptionModel.key(DEFAULT_SKETCH_NAME));
    try (var paths = Files.walk(BaseNoGui.getSketchbookFolder().toPath())) {
      paths
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".ino"))
        .filter(path -> !path.getFileName().toString().equals(DEFAULT_SKETCH_NAME))
        .forEach(path -> options.add(OptionModel.of(path.getFileName().toString()).json(node ->
          node.put("removable", true))));
    }
    options.sort((o1, o2) -> {
      if (o1.getTitleOrKey().equals(DEFAULT_SKETCH_NAME)) {
        return -1;
      }
      if (o2.getTitleOrKey().equals(DEFAULT_SKETCH_NAME)) {
        return 1;
      }
      return o1.getTitleOrKey().compareTo(o2.getTitleOrKey());
    });
    return options;
  }

  @Override
  public @NotNull Class<String> getType() {
    return String.class;
  }

  @Override
  public boolean rawInput() {
    return true;
  }

  @Override
  public @Nullable Integer getMinWidth() {
    return 150;
  }

  @Override
  public @Nullable Integer getMaxWidth() {
    return 200;
  }

  @Override
  public boolean removableOption(OptionModel optionModel) {
    ObjectNode params = optionModel.getJson();
    return params != null && params.has("removable");
  }

  @Override
  public void removeOption(Context context, String key) throws Exception {
    Path path = Paths.get(key);
    Files.delete(path);
    context.setting().reloadSettings(getClass());
  }
}
