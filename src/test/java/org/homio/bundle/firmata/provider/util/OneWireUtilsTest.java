package org.homio.bundle.firmata.provider.util;

import static org.homio.bundle.firmata.provider.util.OneWireUtils.to7BitArray;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

public class OneWireUtilsTest {

  @Test
  public void testEncodeDecodeEquals() {
    byte[] input = new byte[]{40, (byte) 219, (byte) 239, 33, 5, 0, 0, 93, 0, 0, 0, 0, 0, 0, 0, 0};
    byte[] encoded = to7BitArray(input);
    ByteBuffer decoded = ByteBuffer.wrap(OneWireUtils.from7BitArray(ByteBuffer.wrap(encoded)));

    assertEquals(decoded, ByteBuffer.wrap(input));
  }

  @Test
  public void testCrc8() {
    ByteBuffer input = ByteBuffer.wrap(new byte[]{0x28, (byte) 0xDB, (byte) 0xEF, 0x21, 0x05, 0x00, 0x00, 0x5D});
    byte crcByte = OneWireUtils.crc8(input.array());

    assertEquals(crcByte, input.get(7));
  }

  @Test
  public void testReadDevices() {
    byte[] input = new byte[]{0x28, (byte) 0xDB, (byte) 0xEF, 0x21, 0x05, 0x00, 0x00, 0x5D};
    List<OneWireDevice> oneWireDevices = OneWireUtils.readDevices(ByteBuffer.wrap(to7BitArray(input)));
    assertEquals(1, oneWireDevices.size());
    OneWireDevice device = oneWireDevices.get(0);
    Object converted = OneWireDevice.toByteArray(device.address);
    assertEquals(ByteBuffer.wrap(input), converted);

    input = new byte[]{0x28, (byte) 0xDB, (byte) 0xEF, 0x21, 0x05, 0x00, 0x00, 0x5D, 0x28, (byte) 0xDB, (byte) 0xEF, 0x21, 0x05, 0x00, 0x00, 0x5D};
    assertEquals(2, OneWireUtils.readDevices(ByteBuffer.wrap(to7BitArray(input))).size());

    input = new byte[]{0x28, (byte) 0xDB, (byte) 0xEF, 0x21, 0x05, 0x00, 0x00, 0x5D, 0x28, (byte) 0xDB, (byte) 0xEF, 0x21, 0x05, 0x00, 0x00, 0x5D, 0x00, 0x01, 0x02};
    assertEquals(2, OneWireUtils.readDevices(ByteBuffer.wrap(to7BitArray(input))).size());
  }
}
