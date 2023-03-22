package org.touchhome.bundle.firmata.arduino.setting;

import cc.arduino.contributions.ProgressListener;
import cc.arduino.contributions.packages.ContributedBoard;
import cc.arduino.contributions.packages.ContributedPackage;
import cc.arduino.contributions.packages.ContributedPlatform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.console.ConsolePlugin;
import org.touchhome.bundle.api.exception.ServerException;
import org.touchhome.bundle.api.setting.SettingPluginPackageInstall;
import org.touchhome.bundle.api.setting.console.ConsoleSettingPlugin;
import org.touchhome.bundle.firmata.arduino.ArduinoConfiguration;
import org.touchhome.bundle.firmata.arduino.ArduinoConsolePlugin;
import org.touchhome.bundle.api.ui.field.ProgressBar;
import processing.app.BaseNoGui;

public class ConsoleArduinoBoardManagerSetting implements SettingPluginPackageInstall, ConsoleSettingPlugin<JSONObject> {

  private static List<ContributedPlatformReleases> contributions;

  public static List<ContributedPlatformReleases> getContributions() {
    if (contributions == null) {
      // update indexes
      ArduinoConfiguration.getContributionInstaller();

      contributions = new ArrayList<>();
      for (ContributedPackage pack : BaseNoGui.indexer.getPackages()) {
        for (ContributedPlatform platform : pack.getPlatforms()) {
          addContribution(platform);
        }
      }
    }
    return contributions;
  }

  private static void addContribution(ContributedPlatform platform) {
    for (ContributedPlatformReleases contribution : contributions) {
      if (!contribution.shouldContain(platform)) {
        continue;
      }
      contribution.add(platform);
      return;
    }

    contributions.add(new ContributedPlatformReleases(platform));
  }

  @Override
  public String getIcon() {
    return "fas fa-tasks";
  }

  @Override
  public PackageContext allPackages(EntityContext entityContext) {
    Collection<PackageModel> bundleEntities = new ArrayList<>();
    if (BaseNoGui.packages != null) {
      for (ContributedPlatformReleases release : getContributions()) {
        bundleEntities.add(buildBundleEntity(release.getReleases().stream().map(ContributedPlatform::getVersion).collect(Collectors.toList()), release.getLatest()));
      }
    }

    return new PackageContext(null, bundleEntities);
  }

  @Override
  public PackageContext installedPackages(EntityContext entityContext) {
    Collection<PackageModel> bundleEntities = new ArrayList<>();
    if (BaseNoGui.packages != null) {
      for (ContributedPlatformReleases release : getContributions()) {
        if (release.getInstalled() != null) {
          bundleEntities.add(buildBundleEntity(null, release.getInstalled()));
        }
      }
    }

    return new PackageContext(null, bundleEntities);
  }

  @Override
  public void installPackage(EntityContext entityContext, PackageRequest packageRequest, ProgressBar progressBar) throws Exception {
    if (BaseNoGui.packages != null) {
      ContributedPlatform platform = searchContributedPlatform(packageRequest.getName(), packageRequest.getVersion());
      ProgressListener progressListener = progress ->
          progressBar.progress(progress.getProgress(), progress.getStatus());
      ArduinoConfiguration.getContributionInstaller().install(platform, progressListener);
      boardUpdated(entityContext);
    }
  }

  @Override
  public void unInstallPackage(EntityContext entityContext, PackageRequest packageRequest, ProgressBar progressBar) throws Exception {
    if (BaseNoGui.packages != null) {
      ContributedPlatformReleases release = getContributedPlatformReleases(packageRequest.getName());
      if (release.getInstalled().isBuiltIn()) {
        throw new RuntimeException("Unable to remove build-in board");
      }
      ArduinoConfiguration.getContributionInstaller().remove(release.getInstalled());
      boardUpdated(entityContext);
    }
  }

  @Override
  public int order() {
    return 80;
  }

  @Override
  public boolean acceptConsolePluginPage(ConsolePlugin consolePlugin) {
    return consolePlugin instanceof ArduinoConsolePlugin;
  }

  private PackageModel buildBundleEntity(List<String> versions, ContributedPlatform latest) {
    String desc = versions == null ? "" : "<pre>Boards included in this package:<br/><br/>" +
        latest.getBoards().stream().map(ContributedBoard::getName).collect(Collectors.joining("<br/>")) + "" +
        "</pre>";
    PackageModel packageModel = new PackageModel()
        .setName(latest.getName())
        .setTitle(latest.getName())
        .setVersions(versions)
        .setVersion(latest.getVersion())
        .setSize(latest.getSize())
        .setWebsite(latest.getParentPackage().getWebsiteURL())
        .setAuthor(latest.getParentPackage().getMaintainer())
        .setCategory(latest.getCategory())
        .setReadme(desc);
    if (latest.isBuiltIn()) {
      packageModel.setTags(Collections.singleton("Built-In")).setRemovable(false);
    }
    return packageModel;
  }

  private void boardUpdated(EntityContext entityContext) throws Exception {
    BaseNoGui.initPackages();
    contributions = null;
    entityContext.ui().reloadWindow("Re-Initialize page after board installation");
  }

  private ContributedPlatform searchContributedPlatform(String name, String version) {
    ContributedPlatformReleases release = getContributedPlatformReleases(name);
    for (ContributedPlatform contributedPlatform : release.getReleases()) {
      if (contributedPlatform.getVersion().equals(version)) {
        return contributedPlatform;
      }
    }
    throw new ServerException("Unable to find board: " + name + " with version: " + version);
  }

  private ContributedPlatformReleases getContributedPlatformReleases(String name) {
    ContributedPlatformReleases release = getContributions().stream().filter(c -> c.getLatest().getName().equals(name)).findFirst().orElse(null);
    if (release == null) {
      throw new ServerException("Unable to find board with name: " + name);
    }
    return release;
  }
}
