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
    private String userId;

    private String pendingUploadInfo;
    private String pendingUploadFilename;
    private long pendingUploadSize;

    public ClientSession(Socket socket) throws IOException {
        this.socket = socket;
        this.transport = new PacketTransport(
                socket.getInputStream(),
                socket.getOutputStream()
        );
    }

    public String getPendingUploadInfo() {
        return pendingUploadInfo;
    }

    public String getPendingUploadFilename() {
        return pendingUploadFilename;
    }

    public long getPendingUploadSize() {
        return pendingUploadSize;
    }

    public void setPendingUploadInfo(String pendingUploadInfo) {
        this.pendingUploadInfo = pendingUploadInfo;
    }

    public void setPendingUploadFilename(String pendingUploadFilename) {
        this.pendingUploadFilename = pendingUploadFilename;
    }

    public void setPendingUploadSize(long pendingUploadSize) {
        this.pendingUploadSize = pendingUploadSize;
    }

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

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    public SessionState getState() { return state; }
    public void setState(SessionState s) {
        String user = (userId == null) ? "anonymous" : userId;
        Logger.info("Session STATE CHANGE: " + state + " -> " + s + " (user=" + user + ")");
        state = s;
    }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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
                        type != PacketType.UPLOAD_COMPLETE) {
                    throw new IOException("Protocol violation: Only transfer commands allowed");
                }
                break;

            case CLOSING:
                throw new IOException("Session is closing");
        }
    }

}
