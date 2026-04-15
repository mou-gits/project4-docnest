package ca.docnest.shared.protocol;

import ca.docnest.shared.model.FileMetadata;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @class PacketParser
 * @brief Provides helper methods for decoding protocol packets.
 *
 * @details
 * The {@code PacketParser} class is responsible for interpreting incoming
 * {@link DataPacket} objects received by the client or server.
 *
 * It supports parsing:
 * <ul>
 *   <li>JSON-based packets into {@link JsonNode} structures</li>
 *   <li>File metadata lists into {@link FileMetadata} objects</li>
 *   <li>Binary transfer chunks into byte arrays</li>
 *   <li>Error packets into {@link ErrorPayload}</li>
 *   <li>Generic packet dispatch based on packet type</li>
 * </ul>
 *
 * This class works together with {@link PacketBuilder} and provides the
 * inverse operation of packet construction.
 *
 * All methods are static, so the class is used as a utility component.
 */
public class PacketParser {

    // ---------------------------------------------------------
    // Parse JSON-based packets
    // ---------------------------------------------------------

    /**
     * @brief Parses a JSON-based packet into a JSON tree.
     *
     * @details
     * This method verifies that the packet command type is one of the protocol
     * commands that uses JSON payloads. It then validates that the payload is
     * not empty and converts the payload bytes into a {@link JsonNode}.
     *
     * Supported JSON packet types include:
     * <ul>
     *   <li>LOGIN / LOGIN_RESPONSE</li>
     *   <li>LIST_FILES / LIST_FILES_RESPONSE</li>
     *   <li>UPLOAD_INIT / UPLOAD_READY / UPLOAD_COMPLETE / UPLOAD_RESULT</li>
     *   <li>DOWNLOAD_INIT / DOWNLOAD_READY / DOWNLOAD_COMPLETE</li>
     *   <li>DELETE_FILE / DELETE_RESPONSE</li>
     *   <li>LOGOUT</li>
     *   <li>ERROR</li>
     * </ul>
     *
     * @param packet The packet to parse.
     *
     * @return Parsed JSON tree representing the payload.
     *
     * @throws IllegalArgumentException If the packet is not JSON-based or the
     *                                  payload is empty.
     */
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

    /**
     * @brief Parses a LIST_FILES_RESPONSE packet into file metadata objects.
     *
     * @details
     * Extracts the {@code files} array from the JSON payload and converts each
     * entry into a {@link FileMetadata} object.
     *
     * Missing fields are replaced with default values:
     * <ul>
     *   <li>String fields → empty string</li>
     *   <li>Numeric fields → zero</li>
     * </ul>
     *
     * If the files array is missing or invalid, an empty list is returned.
     *
     * @param packet The LIST_FILES_RESPONSE packet to parse.
     *
     * @return A list of {@link FileMetadata} records.
     *
     * @throws IllegalArgumentException If the packet JSON payload is null.
     */
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
                        f.has("fileId") ? f.get("fileId").asText() : "",
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

    /**
     * @brief Parses a binary chunk packet.
     *
     * @details
     * Validates that the packet type is either {@code UPLOAD_CHUNK} or
     * {@code DOWNLOAD_CHUNK}, then validates the payload contents.
     *
     * Validation rules:
     * <ul>
     *   <li>Chunk must not be null</li>
     *   <li>Chunk must not be empty</li>
     *   <li>Chunk size must not exceed protocol chunk size</li>
     * </ul>
     *
     * @param packet The chunk packet to parse.
     *
     * @return The raw chunk byte array.
     *
     * @throws IllegalArgumentException If the packet is not a chunk packet or
     *                                  if validation fails.
     */
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
    // Generic dispatcher
    // ---------------------------------------------------------

    /**
     * @brief Parses a packet using automatic type dispatch.
     *
     * @details
     * If the packet is a transfer chunk packet, the payload is returned as
     * {@code byte[]}. Otherwise, the packet is treated as JSON and parsed into
     * a {@link JsonNode}.
     *
     * @param packet The packet to parse.
     *
     * @return Either a {@code byte[]} or {@link JsonNode} depending on type.
     */
    public static Object parse(DataPacket packet) {
        PacketType type = packet.getCommand();

        if (type == PacketType.UPLOAD_CHUNK || type == PacketType.DOWNLOAD_CHUNK) {
            return parseChunk(packet);
        }

        return parseJson(packet);
    }

    /**
     * @brief Parses an ERROR packet into an {@link ErrorPayload}.
     *
     * @details
     * Extracts the fields:
     * <ul>
     *   <li>code</li>
     *   <li>message</li>
     *   <li>details (optional)</li>
     * </ul>
     *
     * @param packet The ERROR packet to parse.
     *
     * @return Parsed {@link ErrorPayload}.
     *
     * @throws IllegalArgumentException If the packet is not an ERROR packet.
     */
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

    /**
     * @brief Parses a LIST_FILES_RESPONSE packet into a string array.
     *
     * @details
     * Reads the {@code files} array from the packet payload and converts each
     * entry into a string value.
     *
     * This method is useful for simplified file lists where only names are
     * required instead of full metadata objects.
     *
     * @param packet The LIST_FILES_RESPONSE packet.
     *
     * @return Array of parsed file names. Returns an empty array if no valid
     *         list is present.
     *
     * @throws IllegalArgumentException If the packet type is invalid.
     */
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