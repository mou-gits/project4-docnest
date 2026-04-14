package ca.docnest.shared.protocol;
/**
 * Packet Semantics:
 *
 * - filename: user-visible file name (provided by client)
 * - fileId: internal unique identifier (UUID, server-side only)
 *
 * Client interacts using filename.
 * Server resolves filename → fileId via metadata.
 *
 * All storage operations MUST use fileId.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PacketBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();
    public static final int CHUNK_SIZE = 4096;
    public static DataPacket buildErrorPacket(int code, String message, String details)
    {
        ErrorPayload payload = new ErrorPayload(code, message, details);
        return buildJsonPacket(PacketType.ERROR, payload);
    }

    // ---------------------------------------------------------
    // JSON → byte[]
    // ---------------------------------------------------------
    public static byte[] jsonToBytes(Object obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    // ---------------------------------------------------------
    // byte[] → JSON tree
    // ---------------------------------------------------------
    public static JsonNode bytesToJson(byte[] data) {
        try {
            return mapper.readTree(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    // ---------------------------------------------------------
    // Build a JSON-based DataPacket
    // ---------------------------------------------------------
    public static DataPacket buildJsonPacket(PacketType type, Object jsonObject) {
        byte[] payload = jsonToBytes(jsonObject);
        return new DataPacket(type, payload);
    }
    // ---------------------------------------------------------
    // Split a byte[] into 4096-byte chunks
    // ---------------------------------------------------------
    public static List<byte[]> splitIntoChunks(byte[] fileData) {
        List<byte[]> chunks = new ArrayList<>();

        int offset = 0;
        while (offset < fileData.length) {
            int remaining = fileData.length - offset;
            int size = Math.min(CHUNK_SIZE, remaining);

            byte[] chunk = new byte[size];
            System.arraycopy(fileData, offset, chunk, 0, size);

            chunks.add(chunk);
            offset += size;
        }

        return chunks;
    }

    // ---------------------------------------------------------
    // Build a binary chunk packet (UPLOAD_CHUNK or DOWNLOAD_CHUNK)
    // ---------------------------------------------------------

    public static DataPacket buildChunkPacket(PacketType type, byte[] chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk is null");
        }

        if (chunk.length == 0) {
            throw new IllegalArgumentException("Chunk is empty");
        }

        if (chunk.length > CHUNK_SIZE) {
            throw new IllegalArgumentException("Chunk exceeds 4096 bytes");
        }
        return new DataPacket(type, chunk);
    }

    // ---------------------------------------------------------
    // Validate a received chunk
    // ---------------------------------------------------------

    public static boolean isValidChunk(byte[] chunk) {
        return chunk != null && chunk.length > 0 && chunk.length <= CHUNK_SIZE;
    }

    // ---------------------------------------------------------
    // LOGIN / LOGIN_RESPONSE
    // ---------------------------------------------------------

    public static DataPacket buildLoginPacket(String username, String password) {
        var json = Map.of(
                "username", username,
                "password", password
        );
        return buildJsonPacket(PacketType.LOGIN, json);
    }

    public static DataPacket buildLoginResponsePacket(boolean success, String message) {
        var json = Map.of(
                "success", success,
                "message", message
        );
        return buildJsonPacket(PacketType.LOGIN_RESPONSE, json);
    }

    // ---------------------------------------------------------
    // LIST_FILES / LIST_FILES_RESPONSE
    // ---------------------------------------------------------

    public static DataPacket buildListFilesPacket() {
        return buildJsonPacket(PacketType.LIST_FILES, Map.of());
    }


    public static DataPacket buildListFilesResponsePacket(java.util.List<?> records) {
        return buildJsonPacket(
                PacketType.LIST_FILES_RESPONSE,
                java.util.Map.of("files", records)
        );
    }
    // ---------------------------------------------------------
    // UPLOAD_INIT / UPLOAD_READY
    // ---------------------------------------------------------

    public static DataPacket buildUploadInitPacket(String filename, long size, String info) {
        var json = Map.of(
                "filename", filename,
                "size", size,
                "info", info == null ? "" : info
        );
        return buildJsonPacket(PacketType.UPLOAD_INIT, json);
    }

    public static DataPacket buildUploadReadyPacket() {
        return buildJsonPacket(PacketType.UPLOAD_READY, Map.of());
    }

    // ---------------------------------------------------------
    // UPLOAD_CHUNK / UPLOAD_COMPLETE / UPLOAD_RESULT
    // ---------------------------------------------------------

    public static DataPacket buildUploadChunkPacket(byte[] chunk) {
        return buildChunkPacket(PacketType.UPLOAD_CHUNK, chunk);
    }

    public static DataPacket buildUploadCompletePacket() {
        return buildJsonPacket(PacketType.UPLOAD_COMPLETE, Map.of());
    }

    public static DataPacket buildUploadResultPacket(boolean success, String message) {
        var json = Map.of(
                "success", success,
                "message", message
        );
        return buildJsonPacket(PacketType.UPLOAD_RESULT, json);
    }

    // ---------------------------------------------------------
    // UPLOAD_CHUNK / UPLOAD_COMPLETE / UPLOAD_RESULT
    // ---------------------------------------------------------

    public static DataPacket buildDownloadInitPacket(String filename) {
        var json = Map.of(
                "filename", filename
        );
        return buildJsonPacket(PacketType.DOWNLOAD_INIT, json);
    }

    public static DataPacket buildDownloadReadyPacket(String filename, long size) {
        var json = Map.of(
                "filename", filename,
                "size", size
        );
        return buildJsonPacket(PacketType.DOWNLOAD_READY, json);
    }

    public static DataPacket buildDownloadChunkPacket(byte[] chunk) {
        return buildChunkPacket(PacketType.DOWNLOAD_CHUNK, chunk);
    }

    public static DataPacket buildDownloadCompletePacket(String filename) {
        var json = Map.of(
                "filename", filename
        );
        return buildJsonPacket(PacketType.DOWNLOAD_COMPLETE, json);
    }

    public static DataPacket buildDownloadCompletePacket() {
        return buildJsonPacket(PacketType.DOWNLOAD_COMPLETE, Map.of());
    }

    // ---------------------------------------------------------
    // DELETE_FILE / DELETE_RESPONSE
    // ---------------------------------------------------------

    public static DataPacket buildDeleteFilePacket(String filename) {
        var json = Map.of(
                "filename", filename
        );
        return buildJsonPacket(PacketType.DELETE_FILE, json);
    }

    public static DataPacket buildDeleteResponsePacket(boolean success, String message) {
        var json = Map.of(
                "success", success,
                "message", message
        );
        return buildJsonPacket(PacketType.DELETE_RESPONSE, json);
    }

    // ---------------------------------------------------------
    // LOGOUT
    // ---------------------------------------------------------

    public static DataPacket buildLogoutPacket() {
        return buildJsonPacket(PacketType.LOGOUT, Map.of());
    }

}
