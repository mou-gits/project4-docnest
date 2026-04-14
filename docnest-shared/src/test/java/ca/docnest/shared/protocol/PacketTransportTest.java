package ca.docnest.shared.protocol;

import org.junit.jupiter.api.Test;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class PacketTransportTest {

    @Test
    void testSendReceive() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PacketTransport sender = new PacketTransport(null, out);

        DataPacket p = PacketBuilder.buildLoginPacket("bob", "123");
        sender.send(p);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        PacketTransport receiver = new PacketTransport(in, null);

        DataPacket read = receiver.receive();

        assertEquals(PacketType.LOGIN, read.getCommand());
    }
}
