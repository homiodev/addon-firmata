package processing.app;

import cc.arduino.Constants;
import cc.arduino.i18n.Languages;
import org.apache.commons.compress.utils.IOUtils;
import processing.app.helpers.PreferencesHelper;
import processing.app.helpers.PreferencesMap;
import processing.app.legacy.PApplet;
import processing.app.legacy.PConstants;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.stream.Collectors;

import static processing.app.I18n.format;
import static processing.app.I18n.tr;


public class PreferencesData {

  private static final String PREFS_FILE = "preferences.txt";

  // data model

  static PreferencesMap defaults;
  static PreferencesMap prefs = new PreferencesMap();
  static File preferencesFile;
  static boolean doSave = true;


  static public void init(File file) throws Exception {
    if (file == null) {
      BaseNoGui.getPlatform().fixSettingsLocation();
    }
    if (file != null) {
      preferencesFile = file;
    } else {
      preferencesFile = BaseNoGui.getSettingsFile(PREFS_FILE);
    }

    try {
      BaseNoGui.getPlatform().fixPrefsFilePermissions(preferencesFile);
    } catch (Exception e) {
      //ignore
    }

    // Start with a clean slate
    prefs = new PreferencesMap();

    // start by loading the defaults, in case something
    // important was deleted from the user prefs
    try {
      prefs.load(new File(BaseNoGui.getContentFile("lib"), PREFS_FILE));
    } catch (IOException e) {
      BaseNoGui.showError(null, tr("Could not read default settings.\n" +
                                   "You'll need to reinstall Arduino."), e);
    }

    // set some runtime constants (not saved on preferences file)
    File hardwareFolder = BaseNoGui.getHardwareFolder();
    prefs.put("runtime.ide.path", hardwareFolder.getParentFile().getAbsolutePath());
    prefs.put("runtime.ide.version", "" + BaseNoGui.REVISION);

    // clone the hash table
    defaults = new PreferencesMap(prefs);

    if (preferencesFile.exists()) {
      // load the previous preferences file
      try {
        prefs.load(preferencesFile);
      } catch (IOException ex) {
        BaseNoGui.showError(tr("Error reading preferences"),
          I18n.format(tr("Error reading the preferences file. "
                         + "Please delete (or move)\n"
                         + "{0} and restart Arduino."),
            preferencesFile.getAbsolutePath()), ex);
      }
    }

    // load the I18n module for internationalization
    String lang = get("editor.languages.current");
    if (lang == null || !Languages.have(lang)) {
      lang = "";
      set("editor.languages.current", "");
    }
    try {
      I18n.init(lang);
    } catch (MissingResourceException e) {
      I18n.init("en");
      set("editor.languages.current", "en");
    }

    // set some other runtime constants (not saved on preferences file)
    set("runtime.os", PConstants.platformNames[PApplet.platform]);

    fixPreferences();
  }

  public static File getPreferencesFile() {
    return preferencesFile;
  }

  private static void fixPreferences() {
    String baud = get("serial.debug_rate");
    if ("14400".equals(baud) || "28800".equals(baud)) {
      set("serial.debug_rate", "9600");
    }
  }


  static protected void save() {
    if (!doSave)
      return;

    if (getBoolean("preferences.readonly"))
      return;

    // on startup, don't worry about it
    // this is trying to update the prefs for who is open
    // before Preferences.init() has been called.
    if (preferencesFile == null) return;

    // Fix for 0163 to properly use Unicode when writing preferences.txt
    PrintWriter writer = null;
    try {
      writer = PApplet.createWriter(preferencesFile);

      String[] keys = prefs.keySet().toArray(new String[0]);
      Arrays.sort(keys);
      for (String key : keys) {
        if (key.startsWith("runtime."))
          continue;
        writer.println(key + "=" + prefs.get(key));
      }

      writer.flush();
    } catch (Throwable e) {
      System.err.println(format(tr("Could not write preferences file: {0}"), e.getMessage()));
      return;
    } finally {
      IOUtils.closeQuietly(writer);
    }

    try {
      BaseNoGui.getPlatform().fixPrefsFilePermissions(preferencesFile);
    } catch (Exception e) {
      //ignore
    }
  }


  // .................................................................

  static public String get(String attribute) {
    return prefs.get(attribute);
  }

  static public String get(String attribute, String defaultValue) {
    String value = get(attribute);
    return (value == null) ? defaultValue : value;
  }

  static public String getNonEmpty(String attribute, String defaultValue) {
    String value = get(attribute, defaultValue);
    return ("".equals(value)) ? defaultValue : value;
  }

  public static boolean has(String key) {
    return prefs.containsKey(key);
  }

  public static void remove(String key) {
    prefs.remove(key);
  }

  static public String getDefault(String attribute) {
    return defaults.get(attribute);
  }


  static public void set(String attribute, String value) {
    prefs.put(attribute, value);
  }


  static public void unset(String attribute) {
    prefs.remove(attribute);
  }

  static public boolean getBoolean(String attribute, boolean defaultValue) {
    if (has(attribute)) {
      return getBoolean(attribute);
    }

    return defaultValue;
  }

  static public boolean getBoolean(String attribute) {
    return prefs.getBoolean(attribute);
  }


  static public void setBoolean(String attribute, boolean value) {
    prefs.putBoolean(attribute, value);
  }


  static public int getInteger(String attribute) {
    return Integer.parseInt(get(attribute));
  }

  static public int getInteger(String attribute, int defaultValue) {
    if (has(attribute)) {
      return getInteger(attribute);
    }

    return defaultValue;
  }

  static public void setInteger(String key, int value) {
    set(key, String.valueOf(value));
  }

  static public float getFloat(String attribute, float defaultValue) {
    if (has(attribute)) {
      return getFloat(attribute);
    }

    return defaultValue;
  }

  static public float getFloat(String attribute) {
    return Float.parseFloat(get(attribute));
  }

  // get a copy of the Preferences
  static public PreferencesMap getMap() {
    return new PreferencesMap(prefs);
  }

  static public void removeAllKeysWithPrefix(String prefix) {
    Iterator<String> keys = prefs.keySet().iterator();
    while (keys.hasNext())
      if (keys.next().startsWith(prefix))
        keys.remove();
  }

  // Decide wether changed preferences will be saved. When value is
  // false, Preferences.save becomes a no-op.
  static public void setDoSave(boolean value) {
    doSave = value;
  }

  static public Font getFont(String attr) {
    Font font = PreferencesHelper.getFont(prefs, attr);
    if (font == null) {
      String value = defaults.get(attr);
      prefs.put(attr, value);
      font = PreferencesHelper.getFont(prefs, attr);
    }
    return font;
  }

  public static Collection<String> getCollection(String key) {
    return Arrays.stream(get(key, "").split(","))
      // Remove empty strings from the collection
      .filter((v) -> !v.trim().isEmpty())
      .collect(Collectors.toList());
  }

  public static void setCollection(String key, Collection<String> values) {
    String value = values.stream().collect(Collectors.joining(","));
    set(key, value);
  }

  public static boolean areInsecurePackagesAllowed() {
    if (getBoolean(Constants.ALLOW_INSECURE_PACKAGES, false)) {
      return true;
    }
    return getBoolean(Constants.PREF_CONTRIBUTIONS_TRUST_ALL, false);
  }
}
