/*
 TargetPlatform - Represents a hardware platform
 Part of the Arduino project - http://www.arduino.cc/

 Copyright (c) 2014 Arduino

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
package processing.app.debug;

import processing.app.helpers.PreferencesMap;

import java.io.File;
import java.util.Map;
import java.util.Set;

public interface TargetPlatform {

  String getId();

  File getFolder();

  /**
   * Get TargetBoards under this TargetPlatform into a Map that maps the board
   * id with the corresponding TargetBoard
   *
   * @return a Map<String, TargetBoard>
   */
  Map<String, TargetBoard> getBoards();

  PreferencesMap getCustomMenus();

  /**
   * Return ids for top level menus
   *
   * @return a Set<String> with the ids of the top level custom menus
   */
  Set<String> getCustomMenuIds();

  /**
   * Get preferences for all programmers
   *
   * @return
   */
  Map<String, PreferencesMap> getProgrammers();

  /**
   * Get preferences for a specific programmer
   *
   * @param programmer
   * @return
   */
  PreferencesMap getProgrammer(String programmer);

  /**
   * Get preferences for a specific tool
   *
   * @param tool
   * @return
   */
  PreferencesMap getTool(String tool);

  /**
   * Return TargetPlatform preferences
   *
   * @return
   */
  PreferencesMap getPreferences();

  /**
   * Get a target board
   *
   * @param boardId
   * @return
   */
  TargetBoard getBoard(String boardId);

  /**
   * Get the TargetPackage that contains this TargetPlatform
   *
   * @return
   */
  TargetPackage getContainerPackage();

  /**
   * Returns true if the platform is installed in a subfolder of the sketchbook
   *
   * @return
   */
  boolean isInSketchbook();
}
