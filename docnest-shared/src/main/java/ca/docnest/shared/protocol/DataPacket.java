package ca.docnest.shared.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @class DataPacket
 * @brief Represents a transferable packet used in the DocNest communication protocol.
 *
 * @details
 * The {@code DataPacket} class is the core message container used for data
 * exchange between the DocNest client and server.
 *
 * Each packet contains:
 * <ul>
 *   <li>A {@link PacketType} command indicating the purpose of the packet</li>
 *   <li>A payload size field describing the number of payload bytes</li>
 *   <li>A binary payload containing structured data or raw file content</li>
 * </ul>
 *
 * Packet binary structure:
 * <pre>
 * +----------------------+----------------------+----------------------+
 * | Command ID (4 bytes) | Payload Size (4 bytes) | Payload (N bytes) |
 * +----------------------+----------------------+----------------------+
 * </pre>
 *
 * The class supports both:
 * <ul>
 *   <li>Serialization to an {@link OutputStream}</li>
 *   <li>Deserialization from an {@link InputStream}</li>
 * </ul>
 *
 * This packet format provides a simple and efficient protocol layer for custom
 * network communication.
 */
public class DataPacket {

    /**
     * @brief Command type carried by this packet.
     *
     * @details
     * Identifies the action or message purpose, such as login, upload, error,
     * file listing, or download.
     */
    private final PacketType command;

    /**
     * @brief Number of bytes contained in the payload.
     *
     * @details
     * Automatically derived from the payload length during construction.
     */
    private final int payloadSize;

    /**
     * @brief Raw payload data stored in this packet.
     *
     * @details
     * May contain JSON data, file chunks, metadata, or may be empty depending
     * on the packet type.
     */
    private final byte[] payload;

    /**
     * @brief Constructs a new data packet.
     *
     * @details
     * Creates a packet using the provided command and payload. If the payload
     * argument is {@code null}, an empty byte array is used instead.
     *
     * The payload size is automatically calculated from the final payload.
     *
     * @param command The packet command type.
     * @param payload The payload bytes, or {@code null} for no payload.
     */
    public DataPacket(PacketType command, byte[] payload) {
        this.command = command;
        this.payload = payload != null ? payload : new byte[0];
        this.payloadSize = this.payload.length;
    }

    /**
     * @brief Returns the command type of this packet.
     *
     * @return The {@link PacketType} associated with this packet.
     */
    public PacketType getCommand() {
        return command;
    }

    /**
     * @brief Returns the payload size in bytes.
     *
     * @return Number of bytes in the payload.
     */
    public int getPayloadSize() {
        return payloadSize;
    }

    /**
     * @brief Returns the raw payload bytes.
     *
     * @return Byte array containing the payload data.
     */
    public byte[] getPayload() {
        return payload;
    }

    // ---------------------------
    // Serialization (write)
    // ---------------------------

    /**
     * @brief Writes this packet to an output stream.
     *
     * @details
     * Serializes the packet in binary format using the following order:
     * <ol>
     *   <li>Command ID as a 4-byte integer</li>
     *   <li>Payload size as a 4-byte integer</li>
     *   <li>Payload bytes (if any)</li>
     * </ol>
     *
     * The stream is flushed after writing completes.
     *
     * @param out The destination output stream.
     *
     * @throws IOException If writing to the stream fails.
     */
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

    /**
     * @brief Reads and constructs a packet from an input stream.
     *
     * @details
     * Deserializes a packet by reading:
     * <ol>
     *   <li>Command ID</li>
     *   <li>Payload size</li>
     *   <li>Payload bytes</li>
     * </ol>
     *
     * Validation rules:
     * <ul>
     *   <li>Command ID must be within the supported protocol range (1 to 17)</li>
     *   <li>Payload size must be non-negative</li>
     *   <li>Payload size must not exceed 20 MB</li>
     * </ul>
     *
     * If validation passes, the command ID is converted into a
     * {@link PacketType} and a new {@code DataPacket} instance is returned.
     *
     * @param in The input stream to read from.
     *
     * @return A newly constructed {@code DataPacket}.
     *
     * @throws IOException If reading fails, the command ID is invalid, or the
     *                     payload size is outside the allowed range.
     */
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