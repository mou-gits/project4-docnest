package ca.docnest.server;

import ca.docnest.shared.protocol.DataPacket;
import ca.docnest.shared.protocol.PacketTransport;
import ca.docnest.shared.protocol.PacketType;

import java.io.IOException;
import java.net.Socket;

public class ClientSession implements Runnable {

    private final Socket socket;
    private final PacketTransport transport;
    private SessionState state = SessionState.CONNECTED;

    public ClientSession(Socket socket) throws IOException {
        this.socket = socket;
        this.transport = new PacketTransport(
                socket.getInputStream(),
                socket.getOutputStream()
        );
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                DataPacket packet = transport.receive();

                // State machine enforcement (Step 13)
                enforceStateRules(packet.getCommand());

                // Route packet (Step 12)
                PacketRouter.route(packet, this, transport);
            }
        } catch (IOException e) {
            System.err.println("Session error: " + e.getMessage());
        } finally {
            close();
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    public SessionState getState() { return state; }
    public void setState(SessionState s) { state = s; }

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
                if (type != PacketType.UPLOAD_CHUNK &&
                        type != PacketType.UPLOAD_COMPLETE &&
                        type != PacketType.DOWNLOAD_CHUNK) {
                    throw new IOException("Protocol violation: Only transfer commands allowed");
                }
                break;

            case CLOSING:
                throw new IOException("Session is closing");
        }
    }

}
