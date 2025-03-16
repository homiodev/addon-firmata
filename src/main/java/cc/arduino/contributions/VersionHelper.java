package cc.arduino.contributions;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.Optional;

public class VersionHelper {

  public static Optional<ComparableVersion> valueOf(String ver) {
    if (ver == null) {
      return Optional.empty();
    }
    try {
      // Allow x.y-something, assuming x.y.0-something
      // Allow x-something, assuming x.0.0-something
      String version = ver;
      String extra = "";
      String[] split = ver.split("[+-]", 2);
      if (split.length == 2) {
        version = split[0];
        extra = ver.substring(version.length()); // includes separator + or -
      }
      String[] parts = version.split("\\.");
      if (parts.length >= 3) {
        return Optional.of(new ComparableVersion(ver));
      }
      if (parts.length == 2) {
        version += ".0";
      }
      if (parts.length == 1) {
        version += ".0.0";
      }
      return Optional.of(new ComparableVersion(version + extra));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static int compare(String a, String b) {
    return valueOf(a).get().compareTo(valueOf(b).get());
  }
}
