package ca.docnest.server;

import ca.docnest.shared.protocol.*;
import ca.docnest.shared.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MetadataStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static Path userFile(String userId) {
        return Paths.get("server-data", userId, "metadata.json");
    }

    private static List<FileMetadata> load(String userId) {
        try {
            Path path = userFile(userId);

            if (!Files.exists(path)) {
                return new ArrayList<>();
            }

            return MAPPER.readValue(path.toFile(),
                    new TypeReference<List<FileMetadata>>() {});
        } catch (Exception e) {
            Logger.error("Failed to load metadata for " + userId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static void save(String userId, List<FileMetadata> records) {
        try {
            Path path = userFile(userId);
            Files.createDirectories(path.getParent());

            MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), records);
        } catch (Exception e) {
            Logger.error("Failed to save metadata for " + userId + ": " + e.getMessage());
        }
    }

    // ---------------- PUBLIC API ----------------

    public static void addFile(String userId, String filename, long size,
                               String mime, String additionalInfo) {

        List<FileMetadata> records = load(userId);

        FileMetadata record = new FileMetadata(
                filename,
                size,
                mime,
                userId,
                LocalDateTime.now().format(TS_FORMAT),
                additionalInfo
        );

        records.add(record);
        save(userId, records);

        Logger.info("Metadata added for file: " + filename);
    }

    public static List<FileMetadata> listFiles(String userId) {
        return load(userId);
    }

    public static FileMetadata getFile(String userId, String filename) {
        return load(userId).stream()
                .filter(f -> f.getFilename().equals(filename))
                .findFirst()
                .orElse(null);
    }

    public static void deleteFile(String userId, String filename) {
        List<FileMetadata> records = load(userId);
        records.removeIf(f -> f.getFilename().equals(filename));
        save(userId, records);

        Logger.info("Metadata deleted for file: " + filename);
    }
}