package ca.docnest.server;

import ca.docnest.server.handlers.Handlers;
import ca.docnest.shared.protocol.*;

/**
 * @class PacketRouter
 * @brief Routes incoming protocol packets to the appropriate server-side handler.
 *
 * @details
 * The {@code PacketRouter} class acts as a central dispatch component in the
 * DocNest server protocol architecture. Its purpose is to inspect the command
 * type of each received {@link DataPacket} and forward that packet to the
 * corresponding handler method in the {@link Handlers} class.
 *
 * Supported packet categories include:
 * <ul>
 *   <li>User authentication</li>
 *   <li>File listing requests</li>
 *   <li>Upload initialization, chunk transfer, and completion</li>
 *   <li>Download initialization and chunk processing</li>
 *   <li>File deletion</li>
 *   <li>Logout handling</li>
 * </ul>
 *
 * If a packet command is not supported, the router sends an error response
 * back to the client using the protocol error packet mechanism.
 *
 * This class contains only static routing behavior and is not intended to be
 * instantiated.
 */
public class PacketRouter {

    /**
     * @brief Routes a received packet to the correct handler based on packet type.
     *
     * @details
     * This method extracts the {@link PacketType} from the incoming
     * {@link DataPacket} and dispatches processing to the matching method in
     * the {@link Handlers} class.
     *
     * Routing behavior:
     * <ul>
     *   <li>{@code LOGIN} → login handler</li>
     *   <li>{@code LIST_FILES} → list files handler</li>
     *   <li>{@code UPLOAD_INIT} → upload initialization handler</li>
     *   <li>{@code UPLOAD_CHUNK} → upload chunk handler</li>
     *   <li>{@code UPLOAD_COMPLETE} → upload completion handler</li>
     *   <li>{@code DOWNLOAD_INIT} → download initialization handler</li>
     *   <li>{@code DOWNLOAD_CHUNK} → download chunk handler</li>
     *   <li>{@code DELETE_FILE} → delete handler</li>
     *   <li>{@code LOGOUT} → logout handler</li>
     * </ul>
     *
     * If the packet type does not match any supported command, an error packet
     * is sent back to the client with an "Unsupported command" message.
     *
     * @param packet The received {@link DataPacket} to route.
     * @param session The active {@link ClientSession} associated with the client.
     * @param transport The {@link PacketTransport} used to send responses.
     *
     * @throws Exception Thrown if any handler fails or if the error response
     *                   cannot be sent.
     */
    public static void route(
            DataPacket packet,
            ClientSession session,
            PacketTransport transport
    ) throws Exception {

        PacketType type = packet.getCommand();

        switch (type) {

            case LOGIN:
                Handlers.handleLogin(packet, session, transport);
                break;

            case LIST_FILES:
                Handlers.handleListFiles(packet, session, transport);
                break;

            case UPLOAD_INIT:
                Handlers.handleUploadInit(packet, session, transport);
                break;

            case UPLOAD_CHUNK:
                Handlers.handleUploadChunk(packet, session, transport);
                break;

            case UPLOAD_COMPLETE:
                Handlers.handleUploadComplete(packet, session, transport);
                break;

            case DOWNLOAD_INIT:
                Handlers.handleDownloadInit(packet, session, transport);
                break;

            case DOWNLOAD_CHUNK:
                Handlers.handleDownloadChunk(packet, session, transport);
                break;

            case DELETE_FILE:
                Handlers.handleDeleteFile(packet, session, transport);
                break;

            case LOGOUT:
                Handlers.handleLogout(packet, session, transport);
                break;

            default:
                sendError(transport, 400, "Unsupported command", type.toString());
        }
    }

    /**
     * @brief Sends a protocol error packet to the client.
     *
     * @details
     * This helper method builds an error response packet using
     * {@link PacketBuilder#buildErrorPacket(int, String, String)}, sends it
     * through the provided {@link PacketTransport}, and records the event in
     * the server log.
     *
     * The error payload includes:
     * <ul>
     *   <li>An integer error code</li>
     *   <li>A short error message</li>
     *   <li>Additional details for debugging or context</li>
     * </ul>
     *
     * @param transport The packet transport used to send the error response.
     * @param code The numeric error code to include in the packet.
     * @param msg The main error message.
     * @param details Additional detail text describing the failure.
     *
     * @throws Exception Thrown if the error packet cannot be built or sent.
     */
    private static void sendError(PacketTransport transport, int code, String msg, String details) throws Exception {
        DataPacket error = PacketBuilder.buildErrorPacket(code, msg, details);
        transport.send(error);
        Logger.error("ERROR sent: " + msg + " (" + details + ")");
    }
}