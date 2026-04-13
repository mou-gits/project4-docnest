package ca.docnest.server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = 9090;

    public static void main(String[] args) {
        Logger.init();
        Logger.info("DocNest Server starting on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket client = serverSocket.accept();
                Logger.info("Client connected: " + client.getRemoteSocketAddress());
                new Thread(new ClientSession(client)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
