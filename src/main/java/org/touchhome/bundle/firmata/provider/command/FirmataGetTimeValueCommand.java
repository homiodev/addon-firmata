package org.touchhome.bundle.firmata.provider.command;

import static org.touchhome.bundle.firmata.provider.command.FirmataCommand.SYSEX_GET_TIME_COMMAND;

import java.nio.ByteBuffer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.touchhome.bundle.firmata.model.FirmataBaseEntity;
import org.touchhome.bundle.firmata.provider.IODeviceWrapper;
import org.touchhome.bundle.firmata.provider.util.THUtil;

@Component
@RequiredArgsConstructor
public class FirmataGetTimeValueCommand implements FirmataCommandPlugin {

  private final LockManager<Long> lockManager = new LockManager<>();

  @Override
  public FirmataCommand getCommand() {
    return SYSEX_GET_TIME_COMMAND;
  }

  public Long waitForValue(FirmataBaseEntity entity, byte messageID) {
    return lockManager.await(entity.getEntityID() + messageID, 10000, null);
  }

  @Override
  public void handle(IODeviceWrapper device, FirmataBaseEntity entity, byte messageID, ByteBuffer payload) {
    lockManager.signalAll(entity.getEntityID() + messageID, THUtil.getLong(payload));
  }

  @Override
  public boolean hasTH() {
    return true;
  }
}
