/*
  OnlyFilesWithExtension - FilenameFilter that accepts only files with a 
  specific extension.
  Part of the Arduino project - http://www.arduino.cc/

  Copyright (c) 2011 Cristian Maglie

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package processing.app.helpers.filefilters;

import java.io.File;
import java.io.FilenameFilter;

public class OnlyFilesWithExtension implements FilenameFilter {

  String[] extensions;

  public OnlyFilesWithExtension(String... ext) {
    this.extensions = ext;
  }

  @Override
  public boolean accept(File dir, String name) {
    for (String ext : extensions) {
      if (name.endsWith(ext)) {
        return true;
      }
    }
    return false;
  }

}
