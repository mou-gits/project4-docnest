package ca.docnest.shared.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorPayloadTest {

    @Test
    void testErrorPacket() {
        DataPacket p = PacketBuilder.buildErrorPacket(401, "Unauthorized", "Bad password");

        ErrorPayload err = PacketParser.parseError(p);

        assertEquals(401, err.getCode());
        assertEquals("Unauthorized", err.getMessage());
        assertEquals("Bad password", err.getDetails());
    }
}
