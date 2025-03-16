package org.homio.addon.firmata.setting;

import org.homio.api.setting.SettingPluginSlider;

public class FirmataWatchDogIntervalSetting implements SettingPluginSlider {

  @Override
  public int getMin() {
    return 1;
  }

  @Override
  public int getMax() {
    return 60;
  }

  @Override
  public int defaultValue() {
    return 10;
  }

  @Override
  public int order() {
    return 100;
  }

  @Override
  public boolean isReverted() {
    return true;
  }
}
