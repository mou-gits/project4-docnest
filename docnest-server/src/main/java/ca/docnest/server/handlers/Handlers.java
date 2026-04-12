package ca.docnest.server.handlers;

import ca.docnest.server.AuthService;
import ca.docnest.server.ClientSession;
import ca.docnest.server.FileStorage;
import ca.docnest.server.SessionState;
import ca.docnest.shared.protocol.DataPacket;
import ca.docnest.shared.protocol.PacketBuilder;
import ca.docnest.shared.protocol.PacketParser;
import ca.docnest.shared.protocol.PacketTransport;
import ca.docnest.shared.protocol.PacketType;

public class Handlers {

    public static void handleLogin(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        var json = PacketParser.parseJson(packet);
        String username = json.get("username").asText();
        String password = json.get("password").asText();

        boolean ok = AuthService.authenticate(username, password);
        if (!ok) {
            transport.send(PacketBuilder.buildErrorPacket(401, "Invalid credentials", username));
            session.setState(SessionState.CLOSING);
            session.close();
            return;
        }

        session.setUserId(username);
        transport.send(PacketBuilder.buildLoginResponsePacket(true, "Login successful"));
        session.setState(SessionState.READY);
    }

    public static void handleListFiles(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        transport.send(PacketBuilder.buildListFilesResponsePacket(FileStorage.listFilesForUser(session.getUserId())));
    }

    public static void handleUploadInit(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        var json = PacketParser.parseJson(packet);
        String filename = json.get("filename").asText();
        long size = json.get("size").asLong();

        FileStorage.beginUpload(session.getUserId(), filename, size);
        transport.send(PacketBuilder.buildUploadReadyPacket(filename));
        session.setState(SessionState.TRANSFERRING);
    }

    public static void handleUploadChunk(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        byte[] chunk = PacketParser.parseChunk(packet);
        FileStorage.appendChunk(session.getUserId(), chunk);
    }

    public static void handleUploadComplete(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        FileStorage.finishUpload(session.getUserId());
        transport.send(PacketBuilder.buildUploadResultPacket(true, "Upload complete"));
        session.setState(SessionState.READY);
    }

    public static void handleDownloadInit(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        var json = PacketParser.parseJson(packet);
        String filename = json.get("filename").asText();

        byte[] file = FileStorage.readFile(session.getUserId(), filename);
        session.setState(SessionState.TRANSFERRING);
        transport.send(PacketBuilder.buildDownloadReadyPacket(filename, file.length));

        for (byte[] chunk : PacketBuilder.splitIntoChunks(file)) {
            transport.send(PacketBuilder.buildChunkPacket(PacketType.DOWNLOAD_CHUNK, chunk));
        }

        transport.send(PacketBuilder.buildDownloadCompletePacket(filename));
        session.setState(SessionState.READY);
    }

    public static void handleDownloadChunk(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        transport.send(PacketBuilder.buildErrorPacket(
                400,
                "Client must not send DOWNLOAD_CHUNK",
                "Server streams chunks automatically"
        ));
    }

    public static void handleDeleteFile(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        var json = PacketParser.parseJson(packet);
        String filename = json.get("filename").asText();

        boolean ok = FileStorage.deleteFile(session.getUserId(), filename);
        if (!ok) {
            transport.send(PacketBuilder.buildErrorPacket(404, "File not found", filename));
            return;
        }

        transport.send(PacketBuilder.buildDeleteResponsePacket(true, "Deleted " + filename));
    }

    public static void handleLogout(DataPacket packet, ClientSession session, PacketTransport transport) {
        session.setState(SessionState.CLOSING);
        session.close();
    }
}
