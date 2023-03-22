/*
 * This file is part of Arduino.
 *
 * Copyright 2015 Arduino LLC (http://www.arduino.cc/)
 *
 * Arduino is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * As a special exception, you may use this file as part of a free software
 * library without restriction.  Specifically, if other files instantiate
 * templates or use macros or inline functions from this file, or you compile
 * this file and link it with other files to produce an executable, this
 * file does not by itself cause the resulting executable to be covered by
 * the GNU General Public License.  This exception does not however
 * invalidate any other reasons why the executable file might be covered by
 * the GNU General Public License.
 */

package org.touchhome.bundle.firmata.arduino.setting;

import cc.arduino.contributions.VersionComparator;
import cc.arduino.contributions.packages.ContributedPackage;
import cc.arduino.contributions.packages.ContributedPlatform;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

public class ContributedPlatformReleases {

  public final ContributedPackage packager;
  public final String arch;
  @Getter
  public final List<ContributedPlatform> releases;
  public final List<String> versions;
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
        .filter(ContributedPlatform::isInstalled).sorted(ContributedPlatform.BUILTIN_AS_LAST).collect(Collectors.toList());
    return installedReleases.isEmpty() ? null : installedReleases.get(0);
  }

  public ContributedPlatform getLatest() {
    LinkedList<ContributedPlatform> contribs = new LinkedList<>(releases);
    final VersionComparator versionComparator = new VersionComparator();
    contribs.sort((contrib1, contrib2) -> versionComparator.compare(contrib1.getParsedVersion(), contrib2.getParsedVersion()));

    if (contribs.isEmpty()) {
      return null;
    }

    return contribs.get(contribs.size() - 1);
  }

  public ContributedPlatform getSelected() {
    return selected;
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
