package org.homio.bundle.firmata.provider;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.firmata.provider.command.FirmataCommandPlugin;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirmataCommandPlugins {

  private final EntityContext entityContext;

  private Map<Byte, FirmataCommandPlugin> firmataCommandPlugins;

  // deferred initialization
  private Map<Byte, FirmataCommandPlugin> getFirmataCommandPlugins() {
    if (firmataCommandPlugins == null) {
      this.firmataCommandPlugins = entityContext.getBeansOfType(FirmataCommandPlugin.class).stream().collect(Collectors.toMap(p -> p.getCommand().getValue(), p -> p));
    }
    return firmataCommandPlugins;
  }

  // for 3-th parts
  public void addFirmataCommandPlugin(FirmataCommandPlugin firmataCommandPlugin) {
    firmataCommandPlugins.putIfAbsent(firmataCommandPlugin.getCommand().getValue(), firmataCommandPlugin);
  }

  public FirmataCommandPlugin getFirmataCommandPlugin(byte commandID) {
    FirmataCommandPlugin commandPlugin = getFirmataCommandPlugins().get(commandID);
    if (commandPlugin == null) {
      throw new IllegalArgumentException("Unable to find RF24CommandPlugin for commandID: " + commandID);
    }
    return commandPlugin;
  }
}
