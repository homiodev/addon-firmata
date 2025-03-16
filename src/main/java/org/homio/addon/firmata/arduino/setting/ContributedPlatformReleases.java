package org.homio.addon.firmata.arduino.setting;

import cc.arduino.contributions.VersionComparator;
import cc.arduino.contributions.packages.ContributedPackage;
import cc.arduino.contributions.packages.ContributedPlatform;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

public class ContributedPlatformReleases {

  public final ContributedPackage packager;
  public final String arch;
  @Getter
  public final List<ContributedPlatform> releases;
  public final List<String> versions;
  @Getter
  public ContributedPlatform selected = null;

  public ContributedPlatformReleases(ContributedPlatform platform) {
    packager = platform.getParentPackage();
    arch = platform.getArchitecture();
    releases = new LinkedList<>();
    versions = new LinkedList<>();
    add(platform);
  }

  public boolean shouldContain(ContributedPlatform platform) {
    if (platform.getParentPackage() != packager) {
      return false;
    }
    return platform.getArchitecture().equals(arch);
  }

  public void add(ContributedPlatform platform) {
    releases.add(platform);
    String version = platform.getParsedVersion();
    if (version != null) {
      versions.add(version);
    }
    selected = getLatest();
  }

  public ContributedPlatform getInstalled() {
    List<ContributedPlatform> installedReleases = releases.stream()
      .filter(ContributedPlatform::isInstalled).sorted(ContributedPlatform.BUILTIN_AS_LAST).toList();
    return installedReleases.isEmpty() ? null : installedReleases.get(0);
  }

  public ContributedPlatform getLatest() {
    var contrib = new LinkedList<>(releases);
    var versionComparator = new VersionComparator();
    contrib.sort((contrib1, contrib2) ->
      versionComparator.compare(contrib1.getParsedVersion(), contrib2.getParsedVersion()));

    if (contrib.isEmpty()) {
      return null;
    }

    return contrib.get(contrib.size() - 1);
  }

  public void select(ContributedPlatform value) {
    for (ContributedPlatform plat : releases) {
      if (plat == value) {
        selected = plat;
        return;
      }
    }
  }
}
