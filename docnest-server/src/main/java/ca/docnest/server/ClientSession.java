package ca.docnest.server;

import ca.docnest.shared.protocol.DataPacket;
import ca.docnest.shared.protocol.PacketTransport;
import ca.docnest.shared.protocol.PacketType;

import java.io.IOException;
import java.net.Socket;

/**
 * @class ClientSession
 * @brief Represents a single connected client session on the DocNest server.
 *
 * @details
 * The {@code ClientSession} class manages communication and lifecycle handling
 * for one connected client socket. Each instance is typically executed in its
 * own thread because it implements {@link Runnable}.
 *
 * Responsibilities include:
 * <ul>
 *   <li>Maintaining the client's socket connection</li>
 *   <li>Receiving packets from the client</li>
 *   <li>Enforcing protocol state-machine rules</li>
 *   <li>Routing packets to the appropriate handlers</li>
 *   <li>Tracking authenticated user identity</li>
 *   <li>Managing temporary upload/download state information</li>
 *   <li>Logging session activity and errors</li>
 * </ul>
 *
 * The session transitions through values of {@link SessionState}, such as:
 * <ul>
 *   <li>CONNECTED</li>
 *   <li>AUTHENTICATING</li>
 *   <li>READY</li>
 *   <li>TRANSFERRING</li>
 *   <li>CLOSING</li>
 * </ul>
 *
 * This class forms a key part of the server-side connection architecture.
 */
public class ClientSession implements Runnable {

    /**
     * @brief Underlying socket connected to the client.
     *
     * @details
     * Used for low-level TCP communication with the remote client.
     */
    private final Socket socket;

    /**
     * @brief Packet transport layer for sending and receiving protocol packets.
     *
     * @details
     * Wraps the socket streams and provides packet-based communication.
     */
    private final PacketTransport transport;

    /**
     * @brief Current lifecycle state of the client session.
     *
     * @details
     * Initialized to {@code CONNECTED} when the session is created and updated
     * as the client authenticates and performs operations.
     */
    private SessionState state = SessionState.CONNECTED;

    /**
     * @brief Authenticated user ID associated with this session.
     *
     * @details
     * This value remains {@code null} until successful login.
     */
    private String userId;

    /**
     * @brief Additional information associated with a pending upload.
     */
    private String pendingUploadInfo;

    /**
     * @brief Filename associated with a pending upload.
     */
    private String pendingUploadFilename;

    /**
     * @brief Expected size in bytes of the pending upload.
     */
    private long pendingUploadSize;

    /**
     * @brief File identifier currently associated with a pending transfer.
     */
    private String pendingFileId;

    /**
     * @brief Returns the pending file identifier.
     *
     * @return The current pending file ID.
     */
    public String getPendingFileId() { return pendingFileId; }

    /**
     * @brief Sets the pending file identifier.
     *
     * @param id The file ID to associate with the current pending operation.
     */
    public void setPendingFileId(String id) { this.pendingFileId = id; }

    /**
     * @brief Constructs a new client session for a connected socket.
     *
     * @details
     * Initializes packet transport using the socket input and output streams.
     *
     * @param socket The connected client socket.
     *
     * @throws IOException If socket streams cannot be accessed.
     */
    public ClientSession(Socket socket) throws IOException {
        this.socket = socket;
        this.transport = new PacketTransport(
                socket.getInputStream(),
                socket.getOutputStream()
        );
    }

    /**
     * @brief Returns pending upload information text.
     *
     * @return Additional upload info.
     */
    public String getPendingUploadInfo() {
        return pendingUploadInfo;
    }

    /**
     * @brief Returns the pending upload filename.
     *
     * @return Filename being uploaded.
     */
    public String getPendingUploadFilename() {
        return pendingUploadFilename;
    }

    /**
     * @brief Returns the expected pending upload size.
     *
     * @return Upload size in bytes.
     */
    public long getPendingUploadSize() {
        return pendingUploadSize;
    }

    /**
     * @brief Sets additional information for the pending upload.
     *
     * @param pendingUploadInfo Additional metadata text.
     */
    public void setPendingUploadInfo(String pendingUploadInfo) {
        this.pendingUploadInfo = pendingUploadInfo;
    }

    /**
     * @brief Sets the filename for the pending upload.
     *
     * @param pendingUploadFilename Name of the file being uploaded.
     */
    public void setPendingUploadFilename(String pendingUploadFilename) {
        this.pendingUploadFilename = pendingUploadFilename;
    }

    /**
     * @brief Sets the expected size of the pending upload.
     *
     * @param pendingUploadSize File size in bytes.
     */
    public void setPendingUploadSize(long pendingUploadSize) {
        this.pendingUploadSize = pendingUploadSize;
    }

    /**
     * @brief Main execution loop for the client session thread.
     *
     * @details
     * Repeatedly waits for incoming packets, logs activity, validates the
     * current protocol state, and routes packets to the packet router.
     *
     * If an error occurs or the client disconnects, the session is closed and
     * cleanup is performed.
     */
    @Override
    public void run() {
        try {
            Logger.info("Session started for " + socket.getRemoteSocketAddress());
            while (!socket.isClosed()) {
                DataPacket packet = transport.receive();

                Logger.info("Received: " + packet.getCommand() +
                        " from " + socket.getRemoteSocketAddress());

                // State machine enforcement (Step 13)
                enforceStateRules(packet.getCommand());

                // Route packet (Step 12)
                PacketRouter.route(packet, this, transport);
            }
        } catch (Exception e) {
            if (socket.isClosed()) {
                Logger.info("Session closed by client.");
            } else {
                Logger.error("Session error: " + e.getMessage());
            }
        } finally {
            Logger.info("Session closed for " + socket.getRemoteSocketAddress());
            close();
        }
    }

    /**
     * @brief Closes the client socket connection.
     *
     * @details
     * Any {@link IOException} thrown during closure is ignored.
     */
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    /**
     * @brief Returns the current session state.
     *
     * @return Current {@link SessionState}.
     */
    public SessionState getState() { return state; }

    /**
     * @brief Updates the session state and logs the transition.
     *
     * @param s The new state to apply.
     */
    public void setState(SessionState s) {
        String user = (userId == null) ? "anonymous" : userId;
        Logger.info("Session STATE CHANGE: " + state + " -> " + s + " (user=" + user + ")");
        state = s;
    }

    /**
     * @brief Returns the authenticated user ID.
     *
     * @return User ID or {@code null} if not authenticated.
     */
    public String getUserId() { return userId; }

    /**
     * @brief Sets the authenticated user ID.
     *
     * @param userId The user ID associated with this session.
     */
    public void setUserId(String userId) { this.userId = userId; }

    /**
     * @brief Enforces valid packet commands based on current session state.
     *
     * @details
     * Implements the server-side protocol state machine. If a packet command is
     * not allowed in the current state, an {@link IOException} is thrown.
     *
     * Rules include:
     * <ul>
     *   <li>CONNECTED: LOGIN required first</li>
     *   <li>READY: Transfer packets not allowed</li>
     *   <li>TRANSFERRING: Only transfer packets allowed</li>
     *   <li>CLOSING: No commands allowed</li>
     * </ul>
     *
     * @param type The incoming packet type to validate.
     *
     * @throws IOException If the packet violates session protocol rules.
     */
    private void enforceStateRules(PacketType type) throws IOException {

        switch (state) {

            case CONNECTED:
                if (type != PacketType.LOGIN) {
                    throw new IOException("Protocol violation: LOGIN required first");
                }
                break;

            case AUTHENTICATING:
                // Only LOGIN_RESPONSE or ERROR should occur here
                // But since server sends LOGIN_RESPONSE, client sends nothing
                break;

            case READY:
                if (type == PacketType.UPLOAD_CHUNK ||
                        type == PacketType.UPLOAD_COMPLETE ||
                        type == PacketType.DOWNLOAD_CHUNK) {
                    throw new IOException("Protocol violation: Not in TRANSFERRING state");
                }
                break;

            case TRANSFERRING:
                Logger.info("TRANSFERRING state accepted packet: " + type);
                // Allow upload flow (client → server)
                if (type == PacketType.UPLOAD_CHUNK ||
                        type == PacketType.UPLOAD_COMPLETE) {
                    break;
                }

                // Allow download flow (server → client streaming)
                if (type == PacketType.DOWNLOAD_CHUNK ||
                        type == PacketType.DOWNLOAD_COMPLETE) {
                    break;
                }

                throw new IOException("Protocol violation: Invalid command during transfer");


            case CLOSING:
                throw new IOException("Session is closing");
        }
    }

}