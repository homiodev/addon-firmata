package org.homio.bundle.firmata.setting;

import static org.homio.bundle.api.util.CommonUtils.MACHINE_IP_ADDRESS;

import java.util.Set;
import org.homio.bundle.api.setting.SettingPlugin;
import org.homio.bundle.api.setting.SettingPluginTextSet;
import org.homio.bundle.hquery.hardware.network.NetworkDescription;

public class FirmataScanPortRangeSetting implements SettingPluginTextSet, SettingPlugin<Set<String>> {

  @Override
  public int order() {
    return 12;
  }

  @Override
  public String getPattern() {
    return NetworkDescription.IP_RANGE_PATTERN;
  }

  @Override
  public String[] defaultValue() {
    return new String[]{MACHINE_IP_ADDRESS.substring(0, MACHINE_IP_ADDRESS.lastIndexOf(".") + 1) + "0-255"};
  }
}
