package ca.docnest.shared.protocol;

import com.fasterxml.jackson.databind.JsonNode;

public class PacketParser {

    // ---------------------------------------------------------
    // Parse JSON-based packets
    // ---------------------------------------------------------
    public static JsonNode parseJson(DataPacket packet) {
        switch (packet.getCommand()) {
            case LOGIN:
            case LOGIN_RESPONSE:
            case LIST_FILES:
            case LIST_FILES_RESPONSE:
            case UPLOAD_INIT:
            case UPLOAD_READY:
            case UPLOAD_COMPLETE:
            case UPLOAD_RESULT:
            case DOWNLOAD_INIT:
            case DOWNLOAD_READY:
            case DOWNLOAD_COMPLETE:
            case DELETE_FILE:
            case DELETE_RESPONSE:
            case LOGOUT:
            case ERROR:

                // JSON VALIDATION GOES HERE
                if (packet.getPayloadSize() == 0) {
                    throw new IllegalArgumentException("JSON packet has empty payload");
                }

                return PacketBuilder.bytesToJson(packet.getPayload());

            default:
                throw new IllegalArgumentException(
                        "Packet type " + packet.getCommand() + " is not JSON-based."
                );
        }
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
}
