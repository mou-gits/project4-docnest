package ca.docnest.client.network;

import ca.docnest.shared.protocol.*;
import java.io.IOException;
import java.net.Socket;

public class ClientNetwork {

    private Socket socket;
    private PacketTransport transport;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        transport = new PacketTransport(
                socket.getInputStream(),
                socket.getOutputStream()
        );
    }

    public PacketTransport getTransport() {
        return transport;
    }

    public void close() {
        try { socket.close(); } catch (Exception ignored) {}
    }

    // -------------------------
    // LOGIN
    // -------------------------
    public boolean login(String username, String password) throws Exception {
        DataPacket loginPacket = PacketBuilder.buildLoginPacket(username, password);
        transport.send(loginPacket);

        DataPacket response = transport.receive();

        if (response.getCommand() == PacketType.LOGIN_RESPONSE) {
            return true;
        }

        if (response.getCommand() == PacketType.ERROR) {
            var err = PacketParser.parseError(response);
            throw new IOException("Login failed: " + err.message());
        }

        throw new IOException("Unexpected response during login");
    }

    // -------------------------
    // LIST FILES
    // -------------------------
    public String[] listFiles() throws Exception {
        DataPacket req = PacketBuilder.buildListFilesPacket();
        transport.send(req);

        DataPacket resp = transport.receive();

        if (resp.getCommand() == PacketType.LIST_FILES_RESPONSE) {
            return PacketParser.parseListFilesResponse(resp);
        }

        throw new IOException("Unexpected response to LIST_FILES");
    }

    // -------------------------
    // UPLOAD
    // -------------------------
    public void upload(String filename, byte[] data) throws Exception {
        // 1. INIT
        DataPacket init = PacketBuilder.buildUploadInitPacket(filename, data.length);
        transport.send(init);

        DataPacket ack = transport.receive();
        if (ack.getCommand() != PacketType.UPLOAD_ACK) {
            throw new IOException("Upload init failed");
        }

        // 2. CHUNKS
        for (byte[] chunk : PacketBuilder.splitIntoChunks(data)) {
            DataPacket chunkPacket = PacketBuilder.buildChunkPacket(PacketType.UPLOAD_CHUNK, chunk);
            transport.send(chunkPacket);

            DataPacket chunkAck = transport.receive();
            if (chunkAck.getCommand() != PacketType.CHUNK_ACK) {
                throw new IOException("Upload chunk failed");
            }
        }

        // 3. COMPLETE
        DataPacket complete = PacketBuilder.buildUploadCompletePacket();
        transport.send(complete);

        DataPacket done = transport.receive();
        if (done.getCommand() != PacketType.UPLOAD_COMPLETE_RESPONSE) {
            throw new IOException("Upload did not complete");
        }
    }

    // -------------------------
    // DOWNLOAD
    // -------------------------
    public byte[] download(String filename) throws Exception {
        DataPacket init = PacketBuilder.buildDownloadInitPacket(filename);
        transport.send(init);

        // META
        DataPacket meta = transport.receive();
        var metaJson = PacketParser.parseJson(meta);
        int size = metaJson.get("size").asInt();

        byte[] buffer = new byte[size];
        int offset = 0;

        // CHUNKS
        while (true) {
            DataPacket p = transport.receive();

            if (p.getCommand() == PacketType.DOWNLOAD_COMPLETE) {
                break;
            }

            if (p.getCommand() != PacketType.DOWNLOAD_CHUNK) {
                throw new IOException("Unexpected packet during download");
            }

            byte[] chunk = PacketParser.parseChunk(p);
            System.arraycopy(chunk, 0, buffer, offset, chunk.length);
            offset += chunk.length;
        }

        return buffer;
    }

    // -------------------------
    // DELETE
    // -------------------------
    public void delete(String filename) throws Exception {
        DataPacket req = PacketBuilder.buildDeleteFilePacket(filename);
        transport.send(req);

        DataPacket resp = transport.receive();

        if (resp.getCommand() == PacketType.ERROR) {
            var err = PacketParser.parseError(resp);
            throw new IOException("Delete failed: " + err.message());
        }

        if (resp.getCommand() != PacketType.DELETE_SUCCESS) {
            throw new IOException("Unexpected response to DELETE_FILE");
        }
    }

    // -------------------------
    // LOGOUT
    // -------------------------
    public void logout() throws Exception {
        DataPacket req = PacketBuilder.buildLogoutPacket();
        transport.send(req);

        DataPacket resp = transport.receive();
        if (resp.getCommand() != PacketType.LOGOUT_RESPONSE) {
            throw new IOException("Unexpected response to LOGOUT");
        }

        close();
    }
}
