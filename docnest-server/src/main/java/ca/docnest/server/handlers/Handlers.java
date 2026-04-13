package ca.docnest.server.handlers;

import ca.docnest.server.*;
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

        Logger.info("LOGIN attempt for user: " + username);

        boolean ok = AuthService.authenticate(username, password);
        if (!ok) {
            Logger.error("LOGIN failed for user: " + username);
            transport.send(PacketBuilder.buildErrorPacket(401, "Invalid credentials", username));
            session.setState(SessionState.CLOSING);
            session.close();
            return;
        }

        Logger.info("LOGIN success for user: " + username);
        session.setUserId(username);
        transport.send(PacketBuilder.buildLoginResponsePacket(true, "Login successful"));
        session.setState(SessionState.READY);
    }

    public static void handleListFiles(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        var records = MetadataStore.listFiles(session.getUserId());
        transport.send(PacketBuilder.buildListFilesResponsePacket(records));
        Logger.info("LIST_FILES for user: " + session.getUserId());
    }

    public static void handleUploadInit(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        var json = PacketParser.parseJson(packet);
        String filename = json.get("filename").asText();
        long size = json.get("size").asLong();

        String additionalInfo = json.has("info") ? json.get("info").asText() : "";

        session.setPendingUploadInfo(additionalInfo);
        session.setPendingUploadFilename(filename);
        session.setPendingUploadSize(size);

        FileStorage.beginUpload(session.getUserId(), filename, size);
        transport.send(PacketBuilder.buildUploadReadyPacket(filename));
        session.setState(SessionState.TRANSFERRING);



        Logger.info("UPLOAD_INIT: " + filename + " (" + size + " bytes) by " + session.getUserId());
    }

    public static void handleUploadChunk(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        byte[] chunk = PacketParser.parseChunk(packet);
        FileStorage.appendChunk(session.getUserId(), chunk);
        Logger.info("UPLOAD_CHUNK received (" + chunk.length + " bytes)");
    }

    public static void handleUploadComplete(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        FileStorage.finishUpload(session.getUserId());
        transport.send(PacketBuilder.buildUploadResultPacket(true, "Upload complete"));
        session.setState(SessionState.READY);
        MetadataStore.addFile(
                session.getUserId(),
                session.getPendingUploadFilename(),
                session.getPendingUploadSize(),
                "application/octet-stream", // keep simple
                session.getPendingUploadInfo()
        );
        Logger.info("UPLOAD_COMPLETE for user: " + session.getUserId());
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
        Logger.info("DOWNLOAD_INIT: " + filename + " by " + session.getUserId());
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
        MetadataStore.deleteFile(session.getUserId(), filename);
        Logger.info("DELETE_FILE: " + filename + " by " + session.getUserId());
    }

    public static void handleLogout(DataPacket packet, ClientSession session, PacketTransport transport) {
        session.setState(SessionState.CLOSING);
        session.close();
        Logger.info("LOGOUT: " + session.getUserId());
    }
}
