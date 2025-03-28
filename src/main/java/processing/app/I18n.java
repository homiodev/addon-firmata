/*
 * by Shigeru KANEMOTO at SWITCHSCIENCE.
 *
 * Extract strings to be translated by:
 *   % xgettext -L Java --from-code=utf-8 -k_ -d Resources_ja *.java
 * Extract and merge by:
 *   % xgettext -j -L Java --from-code=utf-8 -k_ -d Resources_ja *.java
 *
 * Edit "Resources_ja.po".
 * Convert to the properties file format by:
 *   % msgcat -p Resources_ja.po > Resources_ja.properties
 */

package processing.app;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18n {

  public static String PROMPT_YES;
  public static String PROMPT_NO;

  // prompt text stuff
  public static String PROMPT_CANCEL;
  public static String PROMPT_OK;
  public static String PROMPT_BROWSE;
  // start using current locale but still allow using the dropdown list later
  private static ResourceBundle i18n;

  static {
    tr("Arduino");
    tr("Partner");
    tr("Recommended");
    tr("Contributed");
    tr("Retired");

    tr("Display");
    tr("Communication");
    tr("Signal Input/Output");
    tr("Sensors");
    tr("Device Control");
    tr("Timing");
    tr("Data Storage");
    tr("Data Processing");
    tr("Other");
    tr("Uncategorized");
  }

  static protected void init(String language) throws MissingResourceException {
    String[] languageParts = language.split("_");
    Locale locale = Locale.getDefault();
    // both language and country
    if (languageParts.length == 2) {
      locale = new Locale(languageParts[0], languageParts[1]);
      // just language
    } else if (languageParts.length == 1 && !"".equals(languageParts[0])) {
      locale = new Locale(languageParts[0]);
    }
    // there might be a null pointer exception ... most likely will never happen but the jvm gets mad
    Locale.setDefault(locale);
    i18n = ResourceBundle.getBundle("processing.app.i18n.Resources", Locale.getDefault());
    PROMPT_YES = tr("Yes");
    PROMPT_NO = tr("No");
    PROMPT_CANCEL = tr("Cancel");
    PROMPT_OK = tr("OK");
    PROMPT_BROWSE = tr("Browse");
  }

  public static String tr(String s) {
    String res;
    try {
      if (i18n == null)
        res = s;
      else
        res = i18n.getString(s);
    } catch (MissingResourceException e) {
      res = s;
    }

    // The single % is the arguments selector in .PO files.
    // We must put double %% inside the translations to avoid
    // getting .PO processing in the way.
    res = res.replace("%%", "%");

    return res;
  }

  public static String format(String fmt, Object... args) {
    // Single quote is used to escape curly bracket arguments.

    // - Prevents strings fixed at translation time to be fixed again
    fmt = fmt.replace("''", "'");
    // - Replace ' with the escaped version ''
    fmt = fmt.replace("'", "''");

    return MessageFormat.format(fmt, args);
  }

  /**
   * Does nothing.
   * <p>
   * This method is an hack to extract words with gettext tool.
   */
  protected static void unusedStrings() {
    // This word is defined in the "boards.txt".
    tr("Processor");
  }
}
