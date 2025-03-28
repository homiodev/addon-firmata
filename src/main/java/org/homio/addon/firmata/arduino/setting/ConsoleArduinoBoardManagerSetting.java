package org.homio.addon.firmata.arduino.setting;

import cc.arduino.contributions.ProgressListener;
import cc.arduino.contributions.packages.ContributedBoard;
import cc.arduino.contributions.packages.ContributedPackage;
import cc.arduino.contributions.packages.ContributedPlatform;
import org.homio.addon.firmata.arduino.ArduinoConfiguration;
import org.homio.addon.firmata.arduino.ArduinoConsolePlugin;
import org.homio.addon.firmata.arduino.setting.header.ConsoleHeaderArduinoGetBoardsSetting;
import org.homio.api.Context;
import org.homio.api.cache.CachedValue;
import org.homio.api.console.ConsolePlugin;
import org.homio.api.exception.ServerException;
import org.homio.api.model.Icon;
import org.homio.api.setting.SettingPluginPackageInstall;
import org.homio.api.setting.console.ConsoleSettingPlugin;
import org.homio.hquery.ProgressBar;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import processing.app.BaseNoGui;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsoleArduinoBoardManagerSetting implements SettingPluginPackageInstall, ConsoleSettingPlugin<JSONObject> {

  private static List<ContributedPlatformReleases> contributions;
  private static final CachedValue<PackageContext, Context> allPackagesCache =
    new CachedValue<>(Duration.ofHours(24), context -> {
      Collection<PackageModel> bundleEntities = new ArrayList<>();
      if (BaseNoGui.packages != null) {
        for (ContributedPlatformReleases release : getContributions(context)) {
          bundleEntities.add(buildBundleEntity(release.getReleases().stream().map(ContributedPlatform::getVersion).collect(Collectors.toList()), release.getLatest()));
        }
      }

      return new PackageContext(null, bundleEntities);
    });

  public static List<ContributedPlatformReleases> getContributions(@NotNull Context context) {
    if (contributions == null) {
      // update indexes
      ArduinoConfiguration.getContributionInstaller(context);

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
  public Icon getIcon() {
    return new Icon("fas fa-tasks");
  }

  @Override
  public PackageContext allPackages(@NotNull Context context) {
    return allPackagesCache.getValue(context);
  }

  @Override
  public PackageContext installedPackages(@NotNull Context context) {
    Collection<PackageModel> bundleEntities = new ArrayList<>();
    if (BaseNoGui.packages != null) {
      for (ContributedPlatformReleases release : getContributions(context)) {
        if (release.getInstalled() != null) {
          bundleEntities.add(buildBundleEntity(null, release.getInstalled()));
        }
      }
    }

    return new PackageContext(null, bundleEntities);
  }

  @Override
  public void installPackage(@NotNull Context context, @NotNull PackageRequest packageRequest, @NotNull ProgressBar progressBar) throws Exception {
    if (BaseNoGui.packages != null) {
      ContributedPlatform platform = searchContributedPlatform(
        packageRequest.getName(), packageRequest.getVersion(), context);
      ProgressListener progressListener = progress ->
        progressBar.progress(progress.getProgress(), progress.getStatus());
      ArduinoConfiguration.getContributionInstaller(context).install(platform, progressListener);
      boardUpdated(context);
    }
  }

  @Override
  public void unInstallPackage(@NotNull Context context, @NotNull PackageRequest packageRequest, @NotNull ProgressBar progressBar) throws Exception {
    if (BaseNoGui.packages != null) {
      ContributedPlatformReleases release = getContributedPlatformReleases(packageRequest.getName(), context);
      if (release.getInstalled().isBuiltIn()) {
        throw new RuntimeException("Unable to remove build-in board");
      }
      ArduinoConfiguration.getContributionInstaller(context).remove(release.getInstalled());
      boardUpdated(context);
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

  private static PackageModel buildBundleEntity(List<String> versions, ContributedPlatform latest) {
    String desc = versions == null ? "" : "<pre>Boards included in this package:<br/><br/>" +
                                          latest.getBoards().stream().map(ContributedBoard::getName).collect(Collectors.joining("<br/>")) +
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
      packageModel.setTags(Set.of(BUILT_IN_TAG));
      packageModel.setDisableRemovable(true);
    }
    return packageModel;
  }

  private void boardUpdated(Context context) throws Exception {
    BaseNoGui.initPackages();
    contributions = null;
    context.setting().reloadSettings(ConsoleHeaderArduinoGetBoardsSetting.class);
  }

  private ContributedPlatform searchContributedPlatform(String name, String version, Context context) {
    ContributedPlatformReleases release = getContributedPlatformReleases(name, context);
    for (ContributedPlatform contributedPlatform : release.getReleases()) {
      if (contributedPlatform.getVersion().equals(version)) {
        return contributedPlatform;
      }
    }
    throw new ServerException("Unable to find board: " + name + " with version: " + version);
  }

  private ContributedPlatformReleases getContributedPlatformReleases(String name, Context context) {
    ContributedPlatformReleases release = getContributions(context).stream().filter(c -> c.getLatest().getName().equals(name)).findFirst().orElse(null);
    if (release == null) {
      throw new ServerException("Unable to find board with name: " + name);
    }
    return release;
  }
}
