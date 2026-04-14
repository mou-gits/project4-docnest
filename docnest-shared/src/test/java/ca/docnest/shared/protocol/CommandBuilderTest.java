package ca.docnest.shared.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandBuilderTest {

    @Test
    void testLoginPacket() {
        DataPacket p = PacketBuilder.buildLoginPacket("alice", "pass");

        assertEquals(PacketType.LOGIN, p.getCommand());

        JsonNode json = PacketParser.parseJson(p);
        assertEquals("alice", json.get("username").asText());
        assertEquals("pass", json.get("password").asText());
    }
}
