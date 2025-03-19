package org.homio.addon.firmata.arduino.setting;

import cc.arduino.contributions.ProgressListener;
import cc.arduino.contributions.libraries.ContributedLibrary;
import cc.arduino.contributions.libraries.ContributedLibraryReleases;
import cc.arduino.contributions.libraries.LibraryInstaller;
import lombok.SneakyThrows;
import org.homio.addon.firmata.arduino.ArduinoConfiguration;
import org.homio.addon.firmata.arduino.ArduinoConsolePlugin;
import org.homio.api.Context;
import org.homio.api.console.ConsolePlugin;
import org.homio.api.model.Icon;
import org.homio.api.setting.SettingPluginPackageInstall;
import org.homio.api.setting.console.ConsoleSettingPlugin;
import org.homio.hquery.ProgressBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import processing.app.BaseNoGui;
import processing.app.packages.UserLibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ConsoleArduinoLibraryManagerSetting implements SettingPluginPackageInstall, ConsoleSettingPlugin<JSONObject> {

  private static Map<String, ContributedLibraryReleases> releases;

  @Override
  public Icon getIcon() {
    return SettingPluginPackageInstall.super.getIcon();
  }

  @Override
  public int order() {
    return 90;
  }

  @Override
  public boolean acceptConsolePluginPage(ConsolePlugin consolePlugin) {
    return consolePlugin instanceof ArduinoConsolePlugin;
  }

  @Override
  public PackageContext installedPackages(@NotNull Context context) {
    Collection<PackageModel> bundleEntities = new ArrayList<>();
    if (BaseNoGui.packages != null) {
      for (UserLibrary library : BaseNoGui.librariesIndexer.getInstalledLibraries()) {
        bundleEntities.add(buildBundleEntity(library));
      }
    }
    return new PackageContext(null, bundleEntities);
  }

  @Override
  public PackageContext allPackages(@NotNull Context context) {
    releases = null;
    AtomicReference<String> error = new AtomicReference<>();
    Collection<PackageModel> bundleEntities = new ArrayList<>();
    if (BaseNoGui.packages != null) {
      var progress = context.ui().progress().createProgressBar("all-packages", false);
      for (ContributedLibraryReleases release : getReleases(progress, error).values()) {
        ContributedLibrary latest = release.getLatest();
        bundleEntities.add(buildBundleEntity(release.getReleases().stream().map(ContributedLibrary::getVersion).collect(Collectors.toList()), latest));
      }
    }

    return new PackageContext(error.get(), bundleEntities);
  }

  @Override
  public void installPackage(@NotNull Context context, @NotNull PackageRequest packageRequest, @NotNull ProgressBar progressBar) throws Exception {
    if (BaseNoGui.packages != null) {
      LibraryInstaller installer = ArduinoConfiguration.getLibraryInstaller();
      ContributedLibrary lib = searchLibrary(getReleases(progressBar, null), packageRequest.getName(), packageRequest.getVersion());
      List<ContributedLibrary> deps = BaseNoGui.librariesIndexer.getIndex().resolveDependeciesOf(lib);
      boolean depsInstalled = deps.stream().allMatch(l -> l.getInstalledLibrary().isPresent() || l.getName().equals(lib != null ? lib.getName() : null));

      ProgressListener progressListener = progress -> progressBar.progress(progress.getProgress(), progress.getStatus());
      if (!depsInstalled) {
        installer.install(deps, progressListener);
      }
      installer.install(lib, progressListener);
      reBuildLibraries(context, progressBar);
    }
  }

  @Override
  public void unInstallPackage(@NotNull Context context, @NotNull PackageRequest packageRequest, @NotNull ProgressBar progressBar) throws IOException {
    if (BaseNoGui.packages != null) {
      ContributedLibrary lib = getReleases(progressBar, null).values().stream()
        .filter(r -> r.getInstalled().isPresent() && r.getInstalled().get().getName().equals(packageRequest.getName()))
        .map(r -> r.getInstalled().get()).findAny().orElse(null);

      if (lib == null) {
        context.ui().toastr().error("Library '" + packageRequest.getName() + "' not found");
      } else if (lib.isIDEBuiltIn()) {
        context.ui().toastr().error("Unable remove built-in library: '" + packageRequest.getName() + "'");
      } else {
        ProgressListener progressListener = progress -> progressBar.progress(progress.getProgress(), progress.getStatus());
        LibraryInstaller installer = ArduinoConfiguration.getLibraryInstaller();
        installer.remove(lib, progressListener);
        reBuildLibraries(context, progressBar);
      }
    }
  }

  private ContributedLibrary searchLibrary(Map<String, ContributedLibraryReleases> releases, String name, String version) {
    ContributedLibraryReleases release = releases.get(name);
    if (release != null) {
      return release.getReleases().stream().filter(r -> r.getVersion().equals(version)).findAny().orElse(null);
    }
    return null;
  }

  @SneakyThrows
  private PackageModel buildBundleEntity(UserLibrary library) {
    PackageModel packageModel = new PackageModel()
      .setName(library.getName())
      .setTitle(library.getSentence())
      .setVersion(library.getVersion())
      .setWebsite(library.getWebsite())
      .setAuthor(library.getAuthor())
      .setCategory(library.getCategory())
      .setReadme(library.getParagraph());
    String[] readmeFiles = library.getInstalledFolder().list((dir, name) -> name.toLowerCase().startsWith("readme."));
    if (readmeFiles != null && readmeFiles.length > 0) {
      packageModel.setReadme(packageModel.getReadme() + "<br/><br/>" +
                             new String(Files.readAllBytes(library.getInstalledFolder().toPath().resolve(readmeFiles[0]))));
    }

    if (library.isIDEBuiltIn()) {
      packageModel.setTags(Set.of(BUILT_IN_TAG));
      packageModel.setDisableRemovable(true);
    }

    return packageModel;
  }

  private PackageModel buildBundleEntity(List<String> versions, ContributedLibrary library) {
    PackageModel packageModel = new PackageModel()
      .setName(library.getName())
      .setTitle(library.getSentence())
      .setVersions(versions)
      .setVersion(library.getVersion())
      .setSize(library.getSize())
      .setWebsite(library.getWebsite())
      .setAuthor(library.getAuthor())
      .setCategory(library.getCategory())
      .setReadme(library.getParagraph());
    if (library.isIDEBuiltIn()) {
      packageModel.setTags(Set.of(BUILT_IN_TAG));
      packageModel.setDisableRemovable(true);
    }

    return packageModel;
  }

  private void reBuildLibraries(@NotNull Context context, @NotNull ProgressBar progressBar) {
    ConsoleArduinoLibraryManagerSetting.releases = null;
    getReleases(progressBar, null);
    context.ui().dialog().reloadWindow("Re-Initialize page after library installation");
  }

  @SneakyThrows
  private synchronized Map<String, ContributedLibraryReleases> getReleases(@NotNull ProgressBar progressBar, @Nullable AtomicReference<String> error) {
    if (releases == null) {
      releases = new HashMap<>();

      BaseNoGui.onBoardOrPortChange();

      ProgressListener progressListener = progress ->
        progressBar.progress(progress.getProgress(), progress.getStatus());

      try {
        LibraryInstaller libraryInstaller = ArduinoConfiguration.getLibraryInstaller();
        libraryInstaller.updateIndex(progressListener);
      } catch (Exception ex) {
        if (error == null) {
          throw ex;
        }
        error.set(ex.getMessage());
        BaseNoGui.librariesIndexer.parseIndex();
        BaseNoGui.librariesIndexer.rescanLibraries();
      } finally {
        progressBar.done();
      }

      for (ContributedLibrary lib : BaseNoGui.librariesIndexer.getIndex().getLibraries()) {
        if (releases.containsKey(lib.getName())) {
          releases.get(lib.getName()).add(lib);
        } else {
          releases.put(lib.getName(), new ContributedLibraryReleases(lib));
        }
      }
    }
    return releases;
  }
}
