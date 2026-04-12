package ca.docnest.server.handlers;

import ca.docnest.server.ClientSession;
import ca.docnest.server.SessionState;
import ca.docnest.shared.protocol.*;

public class Handlers {

    public static void handleLogin(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {

        var json = PacketParser.parseJson(packet);
        String username = json.get("username").asText();
        String password = json.get("password").asText();

        // TODO: Replace with real authentication
        boolean ok = username.equals("test") && password.equals("123");

        if (!ok) {
            DataPacket err = PacketBuilder.buildErrorPacket(401, "Invalid credentials", username);
            transport.send(err);
            return;
        }

        // Success → send LOGIN_RESPONSE
        DataPacket response = PacketBuilder.buildLoginResponsePacket(username);
        transport.send(response);

        session.setState(SessionState.READY);
    }

    public static void handleListFiles(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {

        // TODO: Replace with real storage
        var files = FileStorage.listFilesForUser(session.getUserId());

        DataPacket response = PacketBuilder.buildListFilesResponse(files);
        transport.send(response);
    }

    public static void handleUploadInit(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {

        var json = PacketParser.parseJson(packet);
        String filename = json.get("filename").asText();
        long size = json.get("size").asLong();

        // Prepare storage
        FileStorage.beginUpload(session.getUserId(), filename, size);

        // Acknowledge
        DataPacket ack = PacketBuilder.buildUploadAckPacket(filename);
        transport.send(ack);

        session.setState(SessionState.TRANSFERRING);
    }


    public static void handleUploadChunk(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {

        byte[] chunk = PacketParser.parseChunk(packet);

        FileStorage.appendChunk(session.getUserId(), chunk);

        // Optional: send CHUNK_ACK
        DataPacket ack = PacketBuilder.buildChunkAckPacket(chunk.length);
        transport.send(ack);
    }

    public static void handleUploadComplete(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {

        FileStorage.finishUpload(session.getUserId());

        DataPacket done = PacketBuilder.buildUploadCompleteResponse();
        transport.send(done);

        session.setState(SessionState.READY);
    }

    public static void handleDownloadInit(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {

        var json = PacketParser.parseJson(packet);
        String filename = json.get("filename").asText();

        byte[] file = FileStorage.readFile(session.getUserId(), filename);

        // Send metadata
        DataPacket meta = PacketBuilder.buildDownloadMetaPacket(filename, file.length);
        transport.send(meta);

        // Send chunks
        for (byte[] chunk : PacketBuilder.splitIntoChunks(file)) {
            DataPacket chunkPacket = PacketBuilder.buildChunkPacket(PacketType.DOWNLOAD_CHUNK, chunk);
            transport.send(chunkPacket);
        }

        // Send completion
        DataPacket done = PacketBuilder.buildDownloadCompletePacket();
        transport.send(done);

        session.setState(SessionState.READY);
    }

    public static void handleDownloadChunk(DataPacket packet,
                                           ClientSession session,
                                           PacketTransport transport) throws Exception {
        // Client should never send this
        DataPacket err = PacketBuilder.buildErrorPacket(
                400,
                "Client must not send DOWNLOAD_CHUNK",
                "Server streams chunks automatically"
        );
        transport.send(err);
    }

    public static void handleDeleteFile(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {

        var json = PacketParser.parseJson(packet);
        String filename = json.get("filename").asText();

        boolean ok = FileStorage.deleteFile(session.getUserId(), filename);

        if (!ok) {
            DataPacket err = PacketBuilder.buildErrorPacket(404, "File not found", filename);
            transport.send(err);
            return;
        }

        DataPacket response = PacketBuilder.buildDeleteSuccessPacket(filename);
        transport.send(response);
    }

    public static void handleLogout(DataPacket packet, ClientSession session, PacketTransport transport) throws Exception {

        DataPacket response = PacketBuilder.buildLogoutResponsePacket();
        transport.send(response);

        session.setState(SessionState.CLOSING);
        session.close();
    }
}
