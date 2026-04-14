package ca.docnest.shared.protocol;

import ca.docnest.shared.model.FileMetadata;
import com.fasterxml.jackson.databind.JsonNode;


import java.util.ArrayList;
import java.util.List;

public class PacketParser {

    // ---------------------------------------------------------
    // Parse JSON-based packets
    // ---------------------------------------------------------
    public static JsonNode parseJson(DataPacket packet) {
        return switch (packet.getCommand()) {
            case LOGIN, LOGIN_RESPONSE, LIST_FILES, LIST_FILES_RESPONSE, UPLOAD_INIT, UPLOAD_READY, UPLOAD_COMPLETE,
                 UPLOAD_RESULT, DOWNLOAD_INIT, DOWNLOAD_READY, DOWNLOAD_COMPLETE, DELETE_FILE, DELETE_RESPONSE, LOGOUT,
                 ERROR -> {

                // JSON VALIDATION GOES HERE
                if (packet.getPayloadSize() == 0) {
                    throw new IllegalArgumentException("JSON packet has empty payload");
                }

                yield PacketBuilder.bytesToJson(packet.getPayload());
            }
            default -> throw new IllegalArgumentException(
                    "Packet type " + packet.getCommand() + " is not JSON-based."
            );
        };
    }

    public static List<FileMetadata> parseFileMetadataList(DataPacket packet) {
        var json = parseJson(packet);
        if (json == null) {
            throw new IllegalArgumentException("LIST_FILES_RESPONSE has null JSON");
        }
        var files = json.get("files");
        if (files == null || !files.isArray()) {
            return new ArrayList<>();
        }

        List<FileMetadata> list = new ArrayList<>();

        if (files != null && files.isArray()) {
            for (var f : files) {
                list.add(new FileMetadata(
                        f.has("filename") ? f.get("filename").asText() : "",
                        f.has("size") ? f.get("size").asLong() : 0,
                        f.has("type") ? f.get("type").asText() : "",
                        f.has("uploadedBy") ? f.get("uploadedBy").asText() : "",
                        f.has("uploadDate") ? f.get("uploadDate").asText() : "",
                        f.has("additionalInfo") ? f.get("additionalInfo").asText() : ""
                ));
            }
        }
        return list;

    }

    // ---------------------------------------------------------
    // Parse binary chunk packets
    // ---------------------------------------------------------
    public static byte[] parseChunk(DataPacket packet) {
        if (packet.getCommand() != PacketType.UPLOAD_CHUNK &&
                packet.getCommand() != PacketType.DOWNLOAD_CHUNK) {
            throw new IllegalArgumentException(
                    "Packet type " + packet.getCommand() + " is not a chunk packet."
            );
        }

        byte[] chunk = packet.getPayload();

        // CHUNK VALIDATION GOES HERE
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk is null");
        }

        if (chunk.length == 0) {
            throw new IllegalArgumentException("Chunk cannot be empty");
        }

        if (!PacketBuilder.isValidChunk(chunk)) {
            throw new IllegalArgumentException("Invalid chunk size: " + chunk.length);
        }
        return chunk;
    }

    // ---------------------------------------------------------
    // Generic dispatcher (optional but useful)
    // ---------------------------------------------------------
    public static Object parse(DataPacket packet) {
        PacketType type = packet.getCommand();

        if (type == PacketType.UPLOAD_CHUNK || type == PacketType.DOWNLOAD_CHUNK) {
            return parseChunk(packet);
        }

        return parseJson(packet);
    }
    public static ErrorPayload parseError(DataPacket packet) {
        if (packet.getCommand() != PacketType.ERROR) {
            throw new IllegalArgumentException("Not an ERROR packet");
        }

        var json = PacketBuilder.bytesToJson(packet.getPayload());

        int code = json.get("code").asInt();
        String message = json.get("message").asText();
        String details = json.has("details") ? json.get("details").asText() : "";

        return new ErrorPayload(code, message, details);
    }

    public static String[] parseListFilesResponse(DataPacket packet) {
        if (packet.getCommand() != PacketType.LIST_FILES_RESPONSE) {
            throw new IllegalArgumentException("Not a LIST_FILES_RESPONSE packet");
        }

        JsonNode json = parseJson(packet);
        JsonNode files = json.get("files");
        if (files == null || !files.isArray()) {
            return new String[0];
        }

        List<String> parsed = new ArrayList<>();
        for (JsonNode file : files) {
            parsed.add(file.asText());
        }
        return parsed.toArray(new String[0]);
    }
}
