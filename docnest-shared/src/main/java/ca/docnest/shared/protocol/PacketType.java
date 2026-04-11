package ca.docnest.shared.protocol;

public enum PacketType {
    LOGIN(1),
    LOGIN_RESPONSE(2),
    LIST_FILES(3),
    LIST_FILES_RESPONSE(4),
    UPLOAD_INIT(5),
    UPLOAD_READY(6),
    UPLOAD_CHUNK(7),
    UPLOAD_COMPLETE(8),
    UPLOAD_RESULT(9),
    DOWNLOAD_INIT(10),
    DOWNLOAD_READY(11),
    DOWNLOAD_CHUNK(12),
    DOWNLOAD_COMPLETE(13),
    DELETE_FILE(14),
    DELETE_RESPONSE(15),
    LOGOUT(16),
    ERROR(17);

    private final int id;

    PacketType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static PacketType fromId(int id) {
        for (PacketType t : values()) {
            if (t.id == id) return t;
        }
        throw new IllegalArgumentException("Unknown PacketType id: " + id);
    }
}
