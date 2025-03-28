/*
 * This file is part of Arduino.
 *
 * Copyright 2017 Arduino LLC (http://www.arduino.cc/)
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
package processing.app.packages;

import processing.app.packages.UserLibraryFolder.Location;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class UserLibraryPriorityComparator implements Comparator<UserLibrary> {

  private final static Map<Location, Integer> priorities = new HashMap<>();

  static {
    priorities.put(Location.SKETCHBOOK, 4);
    priorities.put(Location.CORE, 3);
    priorities.put(Location.REFERENCED_CORE, 2);
    priorities.put(Location.IDE_BUILTIN, 1);
  }

  private final String arch;

  public UserLibraryPriorityComparator(String currentArch) {
    arch = currentArch;
  }

  private boolean hasArchitecturePriority(UserLibrary x) {
    return x.getArchitectures().contains(arch);
  }

  public int priority(UserLibrary l) {
    int priority = priorities.get(l.getLocation());
    if (hasArchitecturePriority(l))
      priority += 10;
    return priority;
  }

  @Override
  public int compare(UserLibrary x, UserLibrary y) {
    if (!x.getName().equals(y.getName())) {
      throw new IllegalArgumentException("The compared libraries must have the same name");
    }
    return priority(x) - priority(y);
  }
}
