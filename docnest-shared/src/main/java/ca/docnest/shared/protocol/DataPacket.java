package ca.docnest.shared.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataPacket {

    private final PacketType command;
    private final int payloadSize;
    private final byte[] payload;

    public DataPacket(PacketType command, byte[] payload) {
        this.command = command;
        this.payload = payload != null ? payload : new byte[0];
        this.payloadSize = this.payload.length;
    }

    public PacketType getCommand() {
        return command;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public byte[] getPayload() {
        return payload;
    }

    // ---------------------------
    // Serialization (write)
    // ---------------------------
    public void writeTo(OutputStream out) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeInt(command.getId());   // 4 bytes
        dos.writeInt(payloadSize);       // 4 bytes

        if (payloadSize > 0) {
            dos.write(payload);          // N bytes
        }

        dos.flush();
    }

    // ---------------------------
    // Deserialization (read)
    // ---------------------------
    public static DataPacket readFrom(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);

        int commandId = dis.readInt();
        int size = dis.readInt();

        // Validate command ID
        if (commandId < 1 || commandId > 17) {
            throw new IOException("Invalid commandId: " + commandId);
        }

        // Validate payload size
        if (size < 0 || size > 20_000_000) { // 20MB max from SRS
            throw new IOException("Invalid payload size: " + size);
        }

        byte[] payload = new byte[size];
        dis.readFully(payload);

        PacketType type = PacketType.fromId(commandId);

        return new DataPacket(type, payload);
    }
}
