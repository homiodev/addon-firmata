package org.homio.addon.firmata.provider.command;

import lombok.Getter;
import org.firmata4j.firmata.parser.FirmataToken;

/**
 * This class contains a set of constants that represent tokens of Firmata protocol.
 */
@Getter
public enum FirmataCommand {
  ONEWIRE_DATA(FirmataToken.ONEWIRE_DATA);

  private final byte value;

  FirmataCommand(int value) {
    this.value = (byte) value;
  }
}
