package ca.docnest.shared.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @class PacketTransport
 * @brief Handles low-level transmission of protocol packets over streams.
 *
 * @details
 * The {@code PacketTransport} class is responsible for sending and receiving
 * {@link DataPacket} objects across a network connection using input and
 * output streams.
 *
 * It acts as the transport layer for the DocNest protocol by:
 * <ul>
 *   <li>Writing packet headers and payloads to an output stream</li>
 *   <li>Reading packet headers and payloads from an input stream</li>
 *   <li>Validating command identifiers and payload sizes</li>
 *   <li>Reconstructing received data into {@link DataPacket} objects</li>
 * </ul>
 *
 * This class is typically used by both client and server components to
 * exchange structured packets over TCP sockets.
 *
 * Packet binary layout:
 * <pre>
 * +----------------------+----------------------+----------------------+
 * | Command ID (4 bytes) | Payload Size (4 bytes) | Payload (N bytes) |
 * +----------------------+----------------------+----------------------+
 * </pre>
 */
public class PacketTransport {

    /**
     * @brief Input stream used to receive packet data.
     *
     * @details
     * Wrapped in a {@link DataInputStream} to simplify reading structured
     * binary values such as integers and payload bytes.
     */
    private final DataInputStream in;

    /**
     * @brief Output stream used to send packet data.
     *
     * @details
     * Wrapped in a {@link DataOutputStream} to simplify writing structured
     * binary values such as integers and payload bytes.
     */
    private final DataOutputStream out;

    /**
     * @brief Constructs a packet transport wrapper around the provided streams.
     *
     * @details
     * The supplied input and output streams are wrapped in
     * {@link DataInputStream} and {@link DataOutputStream} instances to support
     * binary packet transmission.
     *
     * @param in The input stream used for receiving packets.
     * @param out The output stream used for sending packets.
     */
    public PacketTransport(InputStream in, OutputStream out) {
        this.in = new DataInputStream(in);
        this.out = new DataOutputStream(out);
    }

    // ---------------------------------------------------------
    // Send a DataPacket over TCP
    // ---------------------------------------------------------

    /**
     * @brief Sends a {@link DataPacket} over the output stream.
     *
     * @details
     * Serializes the packet in the DocNest binary protocol format:
     * <ol>
     *   <li>Write command ID as a 4-byte integer</li>
     *   <li>Write payload size as a 4-byte integer</li>
     *   <li>Write payload bytes if present</li>
     * </ol>
     *
     * The output stream is flushed after writing to ensure the packet is sent
     * immediately.
     *
     * @param packet The packet to transmit.
     *
     * @throws IOException If an error occurs while writing to the stream.
     */
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

    /**
     * @brief Receives a {@link DataPacket} from the input stream.
     *
     * @details
     * Reads packet data in the expected binary protocol format:
     * <ol>
     *   <li>Read command ID</li>
     *   <li>Read payload size</li>
     *   <li>Validate command ID range</li>
     *   <li>Validate payload size range</li>
     *   <li>Read payload bytes</li>
     *   <li>Construct and return a {@link DataPacket}</li>
     * </ol>
     *
     * Validation rules:
     * <ul>
     *   <li>Command ID must be between 1 and 17</li>
     *   <li>Payload size must be between 0 and 20,000,000 bytes</li>
     * </ul>
     *
     * @return The reconstructed {@link DataPacket}.
     *
     * @throws IOException If reading fails, if the command ID is invalid, or
     *                     if the payload size is outside the allowed range.
     */
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