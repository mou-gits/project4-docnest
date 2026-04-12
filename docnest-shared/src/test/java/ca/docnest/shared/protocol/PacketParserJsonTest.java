package ca.docnest.shared.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketParserJsonTest {

    @Test
    void testParseJson() {
        var json = java.util.Map.of("x", "y");
        DataPacket p = PacketBuilder.buildJsonPacket(PacketType.LOGIN, json);

        JsonNode node = PacketParser.parseJson(p);
        assertEquals("y", node.get("x").asText());
    }

    @Test
    void testEmptyJsonPayload() {
        DataPacket p = new DataPacket(PacketType.LOGIN, new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> PacketParser.parseJson(p));
    }
}
