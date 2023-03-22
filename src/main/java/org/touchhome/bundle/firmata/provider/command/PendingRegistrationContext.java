package org.touchhome.bundle.firmata.provider.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.touchhome.bundle.firmata.model.FirmataBaseEntity;

@Getter
@Setter
@AllArgsConstructor
public class PendingRegistrationContext {

  private FirmataBaseEntity entity;
  private short target;
  private String test;
}
