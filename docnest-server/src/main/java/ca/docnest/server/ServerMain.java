package ca.docnest.server;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    private enum ServerState {
        STARTING,
        LISTENING,
        ACCEPTING,
        CLOSING
    }

    private static ServerState state;

    private static void setState(ServerState newState) {
        Logger.info("SERVER STATE: " + state + " -> " + newState);
        state = newState;
    }

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