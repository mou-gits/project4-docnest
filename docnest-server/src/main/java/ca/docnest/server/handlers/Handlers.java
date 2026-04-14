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

        String fileId = java.util.UUID.randomUUID().toString();


        String additionalInfo = json.has("info") ? json.get("info").asText() : "";

        session.setPendingFileId(fileId);
        session.setPendingUploadInfo(additionalInfo);
        session.setPendingUploadFilename(filename);
        session.setPendingUploadSize(size);

        FileStorage.beginUpload(
                session.getUserId(),
                fileId,
                filename,
                size
        );
        transport.send(PacketBuilder.buildUploadReadyPacket());
        session.setState(SessionState.TRANSFERRING);



        Logger.info("UPLOAD_INIT: " + filename + " (" + size + " bytes) by " + session.getUserId());
    }

    public static void handleUploadChunk(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        byte[] chunk = PacketParser.parseChunk(packet);
        FileStorage.appendChunk(session.getUserId(), chunk);
        Logger.info("UPLOAD_CHUNK received (" + chunk.length + " bytes)");
    }

    public static void handleUploadComplete(
            DataPacket packet,
            ClientSession session,
            PacketTransport transport
    ) throws Exception {

        String userId = session.getUserId();
        String filename = session.getPendingUploadFilename();
        String fileId = session.getPendingFileId(); // ✅ NEW

        try {
            FileStorage.finishUpload(userId);

            try {
                MetadataStore.addFile(
                        userId,
                        fileId,
                        filename,
                        session.getPendingUploadSize(),
                        "application/octet-stream",
                        session.getPendingUploadInfo()
                );

            } catch (Exception metadataError) {

                try {
                    FileStorage.deleteFileById(userId, fileId);
                    Logger.error("Rollback: deleted file due to metadata failure: " + fileId);
                } catch (Exception rollbackError) {
                    Logger.error("Rollback FAILED for fileId: " + fileId +
                            " : " + rollbackError.getMessage());
                }

                throw metadataError;
            }

            // 3. Notify client
            transport.send(PacketBuilder.buildUploadResultPacket(
                    true,
                    "Upload complete"
            ));

            Logger.info("UPLOAD_COMPLETE success for user: " + userId);

        } catch (Exception e) {

            Logger.error("UPLOAD_COMPLETE failed for user: "
                    + userId + " : " + e.getMessage());

            transport.send(PacketBuilder.buildErrorPacket(
                    500,
                    "Upload failed",
                    e.getMessage()
            ));

        } finally {
            session.setState(SessionState.READY);
        }
    }

    public static void handleDownloadInit(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {
        var json = PacketParser.parseJson(packet);

        String filename = json.get("filename").asText();
        String userId = session.getUserId();

        var meta = MetadataStore.findByFilename(userId, filename);

        if (meta == null) {
            transport.send(PacketBuilder.buildErrorPacket(
                    404,
                    "File not found",
                    filename
            ));
            return;
        }

        String fileId = meta.getFileId();

        byte[] file;

        if (fileId == null || fileId.isBlank()) {
            // OLD FILE (pre-UUID)
            file = FileStorage.readFile(userId, filename);
        } else {
            // NEW FILE (UUID-based)
            file = FileStorage.readFileById(userId, fileId);
        }

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

    public static void handleDeleteFile(
            DataPacket packet,
            ClientSession session,
            PacketTransport transport
    ) throws Exception {

        try {
            var json = PacketParser.parseJson(packet);
            String filename = json.get("filename").asText();
            String userId = session.getUserId();

            var meta = MetadataStore.findByFilename(userId, filename);

            if (meta == null) {
                transport.send(PacketBuilder.buildErrorPacket(
                        404,
                        "File not found",
                        filename
                ));
                return;
            }

            String fileId = meta.getFileId();
            boolean ok;

            if (fileId == null || fileId.isBlank()) {
                ok = FileStorage.deleteFile(userId, filename);
            } else {
                ok = FileStorage.deleteFileById(userId, fileId);
            }

            if (!ok) {
                transport.send(PacketBuilder.buildErrorPacket(
                        404,
                        "File not found",
                        filename
                ));
                return;
            }

            // Remove metadata
            MetadataStore.deleteFile(userId, filename);

            // Send success response ONCE
            transport.send(PacketBuilder.buildDeleteResponsePacket(
                    true,
                    "Deleted " + filename
            ));

            Logger.info("DELETE_FILE: " + filename + " by " + userId);

        } catch (Exception e) {

            Logger.error("DELETE_FILE failed: " + e.getMessage());

            transport.send(PacketBuilder.buildErrorPacket(
                    500,
                    "Delete failed",
                    e.getMessage()
            ));
        }
    }
    public static void handleLogout(DataPacket packet, ClientSession session, PacketTransport transport) {
        session.setState(SessionState.CLOSING);
        session.close();
        Logger.info("LOGOUT: " + session.getUserId());
    }
}
