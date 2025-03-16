package org.homio.addon.firmata.setting;

import org.homio.api.setting.SettingPluginTextSet;
import org.homio.hquery.hardware.network.NetworkDescription;

import static org.homio.api.util.HardwareUtils.MACHINE_IP_ADDRESS;

public class FirmataScanPortRangeSetting implements SettingPluginTextSet {

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
