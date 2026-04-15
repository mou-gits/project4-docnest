package ca.docnest.server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * @class ServerMain
 * @brief Entry point for the DocNest server application.
 *
 * @details
 * The {@code ServerMain} class starts and manages the main server lifecycle.
 * It is responsible for:
 * <ul>
 *   <li>Initializing the logging system</li>
 *   <li>Opening the server socket on the configured port</li>
 *   <li>Accepting incoming client connections</li>
 *   <li>Creating a {@link ClientSession} for each connected client</li>
 *   <li>Launching each session in its own thread</li>
 *   <li>Tracking and logging server state transitions</li>
 * </ul>
 *
 * The server listens continuously for new client connections until the process
 * is terminated or an unrecoverable error occurs.
 *
 * The default listening port in this implementation is {@code 9090}.
 */
public class ServerMain {

    /**
     * @enum ServerState
     * @brief Represents the lifecycle states of the server.
     *
     * @details
     * These states are used for logging and monitoring the server lifecycle.
     *
     * Values:
     * <ul>
     *   <li>{@code STARTING} - Server initialization in progress</li>
     *   <li>{@code LISTENING} - Server socket is open and ready</li>
     *   <li>{@code ACCEPTING} - Server is actively accepting connections</li>
     *   <li>{@code CLOSING} - Server shutdown is in progress</li>
     * </ul>
     */
    private enum ServerState {
        STARTING,
        LISTENING,
        ACCEPTING,
        CLOSING
    }

    /**
     * @brief Current runtime state of the server.
     *
     * @details
     * Updated through {@link #setState(ServerState)} whenever the server
     * changes lifecycle phase.
     */
    private static ServerState state;

    /**
     * @brief Updates the current server state and logs the transition.
     *
     * @details
     * Records the previous and new server state using the logging system, then
     * stores the new state as the current state.
     *
     * @param newState The next server state to apply.
     */
    private static void setState(ServerState newState) {
        Logger.info("SERVER STATE: " + state + " -> " + newState);
        state = newState;
    }

    /**
     * @brief Starts the DocNest server.
     *
     * @details
     * This is the main execution method for the server application.
     *
     * Startup workflow:
     * <ol>
     *   <li>Initialize the logger</li>
     *   <li>Set server state to STARTING</li>
     *   <li>Create a {@link ServerSocket} on port 9090</li>
     *   <li>Set server state to LISTENING</li>
     *   <li>Begin accepting client connections</li>
     *   <li>Create a new {@link ClientSession} for each client</li>
     *   <li>Run each session in a separate thread</li>
     * </ol>
     *
     * If an exception occurs, the error is logged. When the application exits,
     * the server transitions to the {@code CLOSING} state.
     *
     * @param args Command-line arguments passed to the server application.
     */
    public static void main(String[] args) {
        int port = 9090;

        Logger.init();

        try {
            // ---- STARTING ----
            setState(ServerState.STARTING);

            try (ServerSocket serverSocket = new ServerSocket(port)) {

                // ---- LISTENING ----
                setState(ServerState.LISTENING);
                Logger.info("Server listening on port " + port);

                // ---- ACCEPTING ----
                setState(ServerState.ACCEPTING);

                while (true) {
                    Socket clientSocket = serverSocket.accept();

                    Logger.info("SERVER EVENT: CONNECTION ACCEPTED from "
                            + clientSocket.getRemoteSocketAddress());

                    ClientSession session = new ClientSession(clientSocket);
                    new Thread(session).start();
                }

            }

        } catch (Exception e) {
            Logger.error("Server error: " + e.getMessage());
        } finally {
            // ---- CLOSING ----
            setState(ServerState.CLOSING);
            Logger.info("Server shutting down.");
        }
    }
}