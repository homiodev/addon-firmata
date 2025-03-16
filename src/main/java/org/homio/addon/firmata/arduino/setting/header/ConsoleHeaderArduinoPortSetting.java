package org.homio.addon.firmata.arduino.setting.header;

import com.fazecast.jSerialComm.SerialPort;
import org.homio.api.model.Icon;
import org.homio.api.setting.SettingPluginOptionsPort;
import org.homio.api.setting.console.header.ConsoleHeaderSettingPlugin;

public class ConsoleHeaderArduinoPortSetting implements ConsoleHeaderSettingPlugin<SerialPort>, SettingPluginOptionsPort {

  @Override
  public Icon getIcon() {
    return new Icon("fab fa-usb", "#8D943B");
  }

  @Override
  public boolean isRequired() {
    return true;
  }

  @Override
  public boolean viewAsButton() {
    return true;
  }
}
