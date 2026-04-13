package ca.docnest.client.network;

import ca.docnest.shared.protocol.DataPacket;
import ca.docnest.shared.protocol.PacketBuilder;
import ca.docnest.shared.protocol.PacketParser;
import ca.docnest.shared.protocol.PacketTransport;
import ca.docnest.shared.protocol.PacketType;
import ca.docnest.shared.model.FileMetadata;

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
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }

    public boolean login(String username, String password) throws Exception {
        transport.send(PacketBuilder.buildLoginPacket(username, password));
        DataPacket response = transport.receive();

        if (response.getCommand() == PacketType.LOGIN_RESPONSE) {
            var json = PacketParser.parseJson(response);
            return json.get("success").asBoolean(false);
        }

        if (response.getCommand() == PacketType.ERROR) {
            var err = PacketParser.parseError(response);
            throw new IOException("Login failed: " + err.getMessage());
        }

        throw new IOException("Unexpected response during login");
    }

    public java.util.List<FileMetadata> listFiles() throws Exception {
        transport.send(PacketBuilder.buildListFilesPacket());
        DataPacket resp = transport.receive();

        if (resp.getCommand() == PacketType.LIST_FILES_RESPONSE) {
            return PacketParser.parseFileMetadataList(resp);
        }

        if (resp.getCommand() == PacketType.ERROR) {
            var err = PacketParser.parseError(resp);
            throw new IOException("List files failed: " + err.getMessage());
        }

        throw new IOException("Unexpected response to LIST_FILES");
    }

    public void upload(String filename, byte[] data, String info) throws Exception {
        transport.send(PacketBuilder.buildUploadInitPacket(filename, data.length,info));

        DataPacket ack = transport.receive();
        if (ack.getCommand() == PacketType.ERROR) {
            var err = PacketParser.parseError(ack);
            throw new IOException("Upload init failed: " + err.getMessage());
        }
        if (ack.getCommand() != PacketType.UPLOAD_READY) {
            throw new IOException("Unexpected response to UPLOAD_INIT");
        }

        for (byte[] chunk : PacketBuilder.splitIntoChunks(data)) {
            transport.send(PacketBuilder.buildChunkPacket(PacketType.UPLOAD_CHUNK, chunk));
        }

        transport.send(PacketBuilder.buildUploadCompletePacket());
        DataPacket done = transport.receive();

        if (done.getCommand() == PacketType.ERROR) {
            var err = PacketParser.parseError(done);
            throw new IOException("Upload failed: " + err.getMessage());
        }
        if (done.getCommand() != PacketType.UPLOAD_RESULT) {
            throw new IOException("Unexpected response to UPLOAD_COMPLETE");
        }
    }

    public byte[] download(String filename) throws Exception {
        transport.send(PacketBuilder.buildDownloadInitPacket(filename));

        DataPacket meta = transport.receive();
        if (meta.getCommand() == PacketType.ERROR) {
            var err = PacketParser.parseError(meta);
            throw new IOException("Download failed: " + err.getMessage());
        }
        if (meta.getCommand() != PacketType.DOWNLOAD_READY) {
            throw new IOException("Unexpected response to DOWNLOAD_INIT");
        }

        var metaJson = PacketParser.parseJson(meta);
        int size = metaJson.get("size").asInt();

        byte[] buffer = new byte[size];
        int offset = 0;

        while (true) {
            DataPacket packet = transport.receive();

            if (packet.getCommand() == PacketType.DOWNLOAD_COMPLETE) {
                break;
            }

            if (packet.getCommand() != PacketType.DOWNLOAD_CHUNK) {
                throw new IOException("Unexpected packet during download");
            }

            byte[] chunk = PacketParser.parseChunk(packet);
            System.arraycopy(chunk, 0, buffer, offset, chunk.length);
            offset += chunk.length;
        }

        return buffer;
    }

    public void delete(String filename) throws Exception {
        transport.send(PacketBuilder.buildDeleteFilePacket(filename));
        DataPacket resp = transport.receive();

        if (resp.getCommand() == PacketType.ERROR) {
            var err = PacketParser.parseError(resp);
            throw new IOException("Delete failed: " + err.getMessage());
        }

        if (resp.getCommand() != PacketType.DELETE_RESPONSE) {
            throw new IOException("Unexpected response to DELETE_FILE");
        }
    }

    public void logout() throws Exception {
        transport.send(PacketBuilder.buildLogoutPacket());
        close();
    }
}
