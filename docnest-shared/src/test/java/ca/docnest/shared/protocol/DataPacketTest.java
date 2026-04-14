package ca.docnest.shared.protocol;

import org.junit.jupiter.api.Test;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class DataPacketTest {

    @Test
    void testWriteReadRoundTrip() throws Exception {
        byte[] payload = "hello".getBytes();
        DataPacket packet = new DataPacket(PacketType.LOGIN, payload);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        packet.writeTo(out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        DataPacket read = DataPacket.readFrom(in);

        assertEquals(PacketType.LOGIN, read.getCommand());
        assertArrayEquals(payload, read.getPayload());
    }

    @Test
    void testInvalidPayloadSize() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{
                0,0,0,1,   // LOGIN
                -1,-1,-1,-1 // negative size
        });

        assertThrows(IOException.class, () -> DataPacket.readFrom(in));
    }
}
