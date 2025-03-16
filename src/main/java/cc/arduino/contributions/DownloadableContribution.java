package cc.arduino.contributions;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.File;
import java.util.Optional;

public abstract class DownloadableContribution {

  private boolean downloaded;
  private File downloadedFile;

  public abstract String getUrl();

  public abstract String getVersion();

  public abstract String getChecksum();

  public abstract long getSize();

  public abstract String getArchiveFileName();

  public boolean isDownloaded() {
    return downloaded;
  }

  public void setDownloaded(boolean downloaded) {
    this.downloaded = downloaded;
  }

  public File getDownloadedFile() {
    return downloadedFile;
  }

  public void setDownloadedFile(File downloadedFile) {
    this.downloadedFile = downloadedFile;
  }

  public String getParsedVersion() {
    Optional<ComparableVersion> version = VersionHelper.valueOf(getVersion());
    return version.map(ComparableVersion::toString).orElse(null);
  }
}
