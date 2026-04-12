package ca.docnest.shared.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketBuilderJsonTest {

    @Test
    void testJsonRoundTrip() {
        var json = java.util.Map.of("a", 123);
        DataPacket p = PacketBuilder.buildJsonPacket(PacketType.LOGIN, json);

        JsonNode node = PacketBuilder.bytesToJson(p.getPayload());
        assertEquals(123, node.get("a").asInt());
    }
}
