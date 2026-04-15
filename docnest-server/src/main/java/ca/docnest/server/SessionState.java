package ca.docnest.server;

/**
 * @enum SessionState
 * @brief Represents the lifecycle states of a client session on the server.
 *
 * @details
 * The {@code SessionState} enumeration defines the valid runtime states used
 * by {@link ClientSession} to control protocol flow and enforce correct client
 * behavior during communication with the DocNest server.
 *
 * Each connected client progresses through these states as operations are
 * performed. The current state determines which packet types are allowed and
 * which actions may be processed.
 *
 * State flow typically follows this sequence:
 * <pre>
 * CONNECTED -> AUTHENTICATING -> READY -> TRANSFERRING -> READY -> CLOSING
 * </pre>
 *
 * State descriptions:
 * <ul>
 *   <li>{@code CONNECTED} - Client has connected but has not yet logged in.</li>
 *   <li>{@code AUTHENTICATING} - Login verification is in progress.</li>
 *   <li>{@code READY} - Client is authenticated and may issue normal commands.</li>
 *   <li>{@code TRANSFERRING} - File upload or download is currently active.</li>
 *   <li>{@code CLOSING} - Session is shutting down and no further commands are allowed.</li>
 * </ul>
 *
 * This enum is primarily used by the server-side state machine logic.
 */
public enum SessionState {

    /**
     * @brief Initial state after a client connection is established.
     *
     * @details
     * The client is connected to the server socket but must authenticate
     * before performing protected operations.
     */
    CONNECTED,

    /**
     * @brief Temporary state while login credentials are being verified.
     *
     * @details
     * The server is processing an authentication request and determining
     * whether the client should be granted access.
     */
    AUTHENTICATING,

    /**
     * @brief Authenticated and idle state ready for commands.
     *
     * @details
     * The client has successfully logged in and may request actions such as
     * listing files, starting uploads, downloading files, or deleting files.
     */
    READY,

    /**
     * @brief Active file transfer state.
     *
     * @details
     * The client is currently engaged in an upload or download operation.
     * Only transfer-related packet commands are valid in this state.
     */
    TRANSFERRING,

    /**
     * @brief Final state indicating session shutdown.
     *
     * @details
     * The connection is being closed or has been marked for termination.
     * No additional client requests should be processed.
     */
    CLOSING
}