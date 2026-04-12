package ca.docnest.shared.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketBuilderChunkTest {

    @Test
    void testSplitIntoChunks() {
        byte[] data = new byte[5000];
        var chunks = PacketBuilder.splitIntoChunks(data);

        assertEquals(2, chunks.size());
        assertEquals(4096, chunks.get(0).length);
        assertEquals(904, chunks.get(1).length);
    }

    @Test
    void testChunkTooLarge() {
        byte[] big = new byte[5000];
        assertThrows(IllegalArgumentException.class,
                () -> PacketBuilder.buildChunkPacket(PacketType.UPLOAD_CHUNK, big));
    }
}
