package ca.docnest.shared.protocol;

/**
 * @enum PacketType
 * @brief Defines all command types used in the DocNest communication protocol.
 *
 * @details
 * The {@code PacketType} enumeration represents the complete set of packet
 * commands exchanged between the DocNest client and server.
 *
 * Each enum constant is associated with a unique numeric identifier used in
 * the binary packet header during network transmission.
 *
 * Packet categories include:
 * <ul>
 *   <li>Authentication commands</li>
 *   <li>File listing requests and responses</li>
 *   <li>Upload lifecycle commands</li>
 *   <li>Download lifecycle commands</li>
 *   <li>File deletion commands</li>
 *   <li>Session logout</li>
 *   <li>Error reporting</li>
 * </ul>
 *
 * These IDs are serialized into packets and later converted back into enum
 * values using {@link #fromId(int)}.
 */
public enum PacketType {

    /**
     * @brief Client login request.
     *
     * @details
     * Sent by the client to submit username and password credentials.
     */
    LOGIN(1),

    /**
     * @brief Server response to a login attempt.
     *
     * @details
     * Indicates whether authentication succeeded or failed.
     */
    LOGIN_RESPONSE(2),

    /**
     * @brief Request to retrieve the user's file list.
     */
    LIST_FILES(3),

    /**
     * @brief Response containing file list data.
     */
    LIST_FILES_RESPONSE(4),

    /**
     * @brief Request to begin a file upload.
     *
     * @details
     * Includes metadata such as filename and size.
     */
    UPLOAD_INIT(5),

    /**
     * @brief Server acknowledgment that upload may begin.
     */
    UPLOAD_READY(6),

    /**
     * @brief Packet containing a chunk of uploaded file data.
     */
    UPLOAD_CHUNK(7),

    /**
     * @brief Indicates that all upload chunks have been sent.
     */
    UPLOAD_COMPLETE(8),

    /**
     * @brief Final upload result response.
     *
     * @details
     * Confirms success or reports failure after upload completion.
     */
    UPLOAD_RESULT(9),

    /**
     * @brief Request to begin downloading a file.
     */
    DOWNLOAD_INIT(10),

    /**
     * @brief Response indicating the requested file is ready for download.
     */
    DOWNLOAD_READY(11),

    /**
     * @brief Packet containing a chunk of downloaded file data.
     */
    DOWNLOAD_CHUNK(12),

    /**
     * @brief Indicates that all download chunks have been sent.
     */
    DOWNLOAD_COMPLETE(13),

    /**
     * @brief Request to delete a stored file.
     */
    DELETE_FILE(14),

    /**
     * @brief Response to a delete request.
     */
    DELETE_RESPONSE(15),

    /**
     * @brief Request to log out and close the session.
     */
    LOGOUT(16),

    /**
     * @brief Packet representing an error condition.
     *
     * @details
     * Carries an {@link ErrorPayload} describing the failure.
     */
    ERROR(17);

    /**
     * @brief Numeric protocol identifier for this packet type.
     *
     * @details
     * Stored in the packet header during serialization.
     */
    private final int id;

    /**
     * @brief Constructs a packet type with its protocol ID.
     *
     * @param id Unique numeric identifier used on the wire.
     */
    PacketType(int id) {
        this.id = id;
    }

    /**
     * @brief Returns the numeric identifier for this packet type.
     *
     * @return The protocol command ID.
     */
    public int getId() {
        return id;
    }

    /**
     * @brief Converts a numeric protocol ID into a {@code PacketType}.
     *
     * @details
     * Iterates through all enum constants and returns the matching packet type.
     *
     * @param id The numeric command identifier received from a packet header.
     *
     * @return The corresponding {@code PacketType}.
     *
     * @throws IllegalArgumentException If the ID does not match any known type.
     */
    public static PacketType fromId(int id) {
        for (PacketType t : values()) {
            if (t.id == id) return t;
        }
        throw new IllegalArgumentException("Unknown PacketType id: " + id);
    }
}