package org.touchhome.bundle.firmata.arduino.setting.header;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.model.OptionModel;
import org.touchhome.bundle.api.setting.SettingPluginOptions;
import org.touchhome.bundle.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.touchhome.bundle.api.ui.field.UIFieldType;
import processing.app.BaseNoGui;

@Log4j2
public class ConsoleHeaderArduinoSketchNameSetting implements SettingPluginOptions<String>, ConsoleHeaderSettingPlugin<String> {

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

  @Override
  public Collection<OptionModel> getOptions(EntityContext entityContext, JSONObject params) {
    /*Collection<OptionModel> options = SettingPluginOptionsFileExplorer.super.getOptions(entityContext, params);
    OptionModel examples = buildExamplePath(false);
    if (examples != null) {
      options.add(OptionModel.separator());
      options.add(examples);
    }
    return options;*/
    return null;
  }

  /*@Override
  public Comparator<OptionModel> pathComparator() {
    return (o1, o2) -> {
      if (o1.getTitleOrKey().equals(DEFAULT_SKETCH_NAME)) {
        return -1;
      }
      if (o2.getTitleOrKey().equals(DEFAULT_SKETCH_NAME)) {
        return 1;
      }
      return o1.getTitleOrKey().compareTo(o2.getTitleOrKey());
    };
  }*/

/*  @Override
  public boolean removableOption(OptionModel optionModel) {
    return !optionModel.getKey().equals(DEFAULT_SKETCH_NAME);
  }

  @Override
  public void removeOption(EntityContext entityContext, String key) throws Exception {
    Path path = parseValue(entityContext, key);
    FileUtils.deleteDirectory(path.getParent().toFile());
    entityContext.setting().reloadSettings(getClass());
  }

  @Override
  public String buildTitle(Path path) {
    return path.getFileName() == null ? path.toString() : path.getFileName().toString();
  }*/

  @Override
  public Class<String> getType() {
    return String.class;
  }

  @Override
  public UIFieldType getSettingType() {
    return UIFieldType.TextSelectBoxDynamic;
  }
}
