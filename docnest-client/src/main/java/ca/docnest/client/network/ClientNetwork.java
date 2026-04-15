package ca.docnest.client.network;

import ca.docnest.shared.protocol.DataPacket;
import ca.docnest.shared.protocol.PacketBuilder;
import ca.docnest.shared.protocol.PacketParser;
import ca.docnest.shared.protocol.PacketTransport;
import ca.docnest.shared.protocol.PacketType;
import ca.docnest.shared.model.FileMetadata;

import java.io.IOException;
import java.net.Socket;

/**
 * @class ClientNetwork
 * @brief Handles client-side network communication with the DocNest server.
 *
 * @details
 * This class provides a high-level interface for establishing a socket
 * connection to the server and performing supported remote operations such as
 * login, file listing, upload, download, deletion, and logout.
 *
 * It encapsulates the lower-level packet communication mechanism by using
 * {@link PacketTransport} for transmission, {@link PacketBuilder} for request
 * packet creation, and {@link PacketParser} for interpreting server responses.
 *
 * The class expects the server to respond with protocol-specific packet types
 * defined in {@link PacketType}. If an unexpected packet type or an explicit
 * error packet is received, the methods throw exceptions to signal failure to
 * the caller.
 *
 * This class is intended to be used by the client application layer as the
 * main communication gateway to the server.
 */
public class ClientNetwork {

    /**
     * @brief Socket used to establish the TCP connection with the server.
     *
     * @details
     * This field stores the active client socket after a successful call to
     * {@link #connect(String, int)}. It remains open until explicitly closed
     * through {@link #close()} or indirectly through {@link #logout()}.
     */
    private Socket socket;

    /**
     * @brief Packet transport wrapper for sending and receiving protocol packets.
     *
     * @details
     * This field is initialized after the socket connection is established. It
     * wraps the socket input and output streams and provides convenient methods
     * for transmitting {@link DataPacket} objects according to the custom
     * application protocol.
     */
    private PacketTransport transport;

    /**
     * @brief Establishes a connection to the DocNest server.
     *
     * @details
     * This method opens a TCP socket to the specified host and port. After the
     * connection is established, it initializes the {@link PacketTransport}
     * object using the socket's input and output streams, enabling packet-based
     * communication for subsequent client requests.
     *
     * @param host The hostname or IP address of the server to connect to.
     * @param port The port number on which the server is listening.
     *
     * @throws IOException Thrown if the socket cannot be opened or if the input
     *                     or output streams cannot be accessed.
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        transport = new PacketTransport(
                socket.getInputStream(),
                socket.getOutputStream()
        );
    }

    /**
     * @brief Returns the active packet transport object.
     *
     * @details
     * This method provides access to the underlying {@link PacketTransport}
     * instance used for packet-based communication. It may be useful when other
     * components need lower-level access to packet send/receive operations.
     *
     * @return The current {@link PacketTransport} instance associated with the
     *         active socket connection.
     */
    public PacketTransport getTransport() {
        return transport;
    }

    /**
     * @brief Closes the current socket connection.
     *
     * @details
     * This method safely closes the socket if it has been initialized. Any
     * exception thrown during socket closure is caught and ignored to prevent
     * disruption during cleanup operations.
     *
     * This method does not throw exceptions to the caller and is intended for
     * graceful shutdown of the network connection.
     */
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * @brief Sends login credentials to the server and checks authentication.
     *
     * @details
     * This method builds and sends a login packet containing the provided
     * username and password. It then waits for the server response.
     *
     * If the server responds with a {@code LOGIN_RESPONSE} packet, the method
     * parses the JSON payload and returns the value of the {@code success}
     * field.
     *
     * If the server responds with an {@code ERROR} packet, the error details
     * are parsed and wrapped in an {@link IOException}.
     *
     * Any packet type other than the expected login response or error response
     * is treated as a protocol violation and results in an exception.
     *
     * @param username The username to authenticate with.
     * @param password The password associated with the given username.
     *
     * @return {@code true} if login succeeds according to the server response;
     *         {@code false} otherwise.
     *
     * @throws Exception Thrown if a communication error occurs, if the server
     *                   returns an error packet, or if an unexpected response is
     *                   received.
     */
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

    /**
     * @brief Requests the list of files available to the authenticated client.
     *
     * @details
     * This method sends a {@code LIST_FILES} request to the server and waits
     * for the corresponding response.
     *
     * If the server returns a {@code LIST_FILES_RESPONSE} packet, the response
     * is parsed into a list of {@link FileMetadata} objects.
     *
     * If the server returns an {@code ERROR} packet, the error details are
     * extracted and thrown as an {@link IOException}.
     *
     * Any unexpected packet type is treated as an invalid protocol response.
     *
     * @return A list of {@link FileMetadata} objects representing the files
     *         available on the server for the current user.
     *
     * @throws Exception Thrown if the request fails, if an error response is
     *                   received, or if the server returns an unexpected packet.
     */
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

    /**
     * @brief Uploads a file and its associated information to the server.
     *
     * @details
     * This method performs a multi-step upload process:
     * <ol>
     *   <li>Sends an upload initialization packet with the filename, total file
     *       size, and additional information.</li>
     *   <li>Waits for an {@code UPLOAD_READY} acknowledgment from the server.</li>
     *   <li>Splits the file data into chunks and sends each chunk as an
     *       {@code UPLOAD_CHUNK} packet.</li>
     *   <li>Sends an {@code UPLOAD_COMPLETE} packet after all chunks have been
     *       transmitted.</li>
     *   <li>Waits for an {@code UPLOAD_RESULT} packet confirming completion.</li>
     * </ol>
     *
     * If an {@code ERROR} packet is received at either the initialization or
     * completion stage, the method parses the error details and throws an
     * exception.
     *
     * Any unexpected response packet at critical steps is treated as a protocol
     * error.
     *
     * @param filename The name of the file to upload.
     * @param data The raw byte content of the file being uploaded.
     * @param info Additional descriptive or metadata information associated with
     *             the file.
     *
     * @throws Exception Thrown if the upload initialization fails, if the server
     *                   reports an error, if packet exchange does not follow the
     *                   expected protocol, or if a communication issue occurs.
     */
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

    /**
     * @brief Downloads a file from the server by filename.
     *
     * @details
     * This method begins by sending a download initialization request for the
     * specified file. The server is expected to respond first with a
     * {@code DOWNLOAD_READY} packet containing metadata, including the total
     * file size.
     *
     * Once the size is known, the method allocates a byte buffer and repeatedly
     * receives {@code DOWNLOAD_CHUNK} packets until a
     * {@code DOWNLOAD_COMPLETE} packet is encountered.
     *
     * Each received chunk is copied into the appropriate position in the result
     * buffer. If an unexpected packet type is received during the transfer, the
     * method throws an exception.
     *
     * @param filename The name of the file to download.
     *
     * @return A byte array containing the full contents of the downloaded file.
     *
     * @throws Exception Thrown if the server reports an error, if the transfer
     *                   protocol is violated, or if a communication problem
     *                   occurs during download.
     */
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

    /**
     * @brief Requests deletion of a file from the server.
     *
     * @details
     * This method sends a delete-file request for the specified filename and
     * waits for the server response.
     *
     * If the server responds with an {@code ERROR} packet, the parsed error
     * message is thrown as an {@link IOException}.
     *
     * If the response is not a {@code DELETE_RESPONSE} packet, the method
     * throws an exception indicating an unexpected protocol response.
     *
     * @param filename The name of the file to delete from the server.
     *
     * @throws Exception Thrown if the server rejects the request, if an error
     *                   packet is returned, or if the response packet type is
     *                   invalid.
     */
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

    /**
     * @brief Logs out the client from the server and closes the network
     * connection.
     *
     * @details
     * This method sends a logout packet to the server using the active transport
     * and then closes the socket connection by calling {@link #close()}.
     *
     * It is intended to terminate the current authenticated session cleanly.
     *
     * @throws Exception Thrown if the logout packet cannot be sent due to a
     *                   communication failure.
     */
    public void logout() throws Exception {
        transport.send(PacketBuilder.buildLogoutPacket());
        close();
    }
}