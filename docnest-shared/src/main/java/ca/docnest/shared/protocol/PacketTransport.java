package ca.docnest.shared.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PacketTransport {

    private final DataInputStream in;
    private final DataOutputStream out;

    public PacketTransport(InputStream in, OutputStream out) {
        this.in = new DataInputStream(in);
        this.out = new DataOutputStream(out);
    }

    // ---------------------------------------------------------
    // Send a DataPacket over TCP
    // ---------------------------------------------------------
    public void send(DataPacket packet) throws IOException {
        out.writeInt(packet.getCommand().getId());   // 4 bytes
        out.writeInt(packet.getPayloadSize());       // 4 bytes

        if (packet.getPayloadSize() > 0) {
            out.write(packet.getPayload());          // N bytes
        }

        out.flush();
    }

    // ---------------------------------------------------------
    // Receive a DataPacket over TCP
    // ---------------------------------------------------------
    public DataPacket receive() throws IOException {
        int commandId = in.readInt();   // 4 bytes
        int size = in.readInt();        // 4 bytes

        if (commandId < 1 || commandId > 17) {
            throw new IOException("Invalid commandId: " + commandId);
        }

        if (size < 0 || size > 20_000_000) { // 20MB max from SRS
            throw new IOException("Invalid payload size: " + size);
        }

        byte[] payload = new byte[size];
        in.readFully(payload);          // N bytes

        PacketType type = PacketType.fromId(commandId);
        return new DataPacket(type, payload);
    }
}
