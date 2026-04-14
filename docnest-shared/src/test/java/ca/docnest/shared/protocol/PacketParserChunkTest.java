package ca.docnest.shared.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketParserChunkTest {

    @Test
    void testParseChunk() {
        byte[] chunk = new byte[100];
        DataPacket p = new DataPacket(PacketType.UPLOAD_CHUNK, chunk);

        byte[] parsed = PacketParser.parseChunk(p);
        assertEquals(100, parsed.length);
    }

    @Test
    void testEmptyChunk() {
        DataPacket p = new DataPacket(PacketType.UPLOAD_CHUNK, new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> PacketParser.parseChunk(p));
    }
}
