package ca.docnest.shared.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketTypeTest {

    @Test
    void testFromIdValid() {
        assertEquals(PacketType.LOGIN, PacketType.fromId(1));
        assertEquals(PacketType.ERROR, PacketType.fromId(17));
    }

    @Test
    void testFromIdInvalid() {
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromId(0));
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromId(99));
    }

    @Test
    void testGetId() {
        assertEquals(1, PacketType.LOGIN.getId());
    }
}
