package ca.docnest.server;

import ca.docnest.shared.protocol.*;
import ca.docnest.shared.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @class MetadataStore
 * @brief Manages file metadata records for users on the DocNest server.
 *
 * @details
 * The {@code MetadataStore} class is responsible for persisting and retrieving
 * metadata associated with uploaded files. Metadata is stored in a
 * {@code metadata.json} file inside each user's server storage directory.
 *
 * Each metadata record is represented by {@link FileMetadata} and may include:
 * <ul>
 *   <li>Internal file ID</li>
 *   <li>Original filename</li>
 *   <li>File size</li>
 *   <li>MIME type</li>
 *   <li>Owner user ID</li>
 *   <li>Upload timestamp</li>
 *   <li>Additional descriptive information</li>
 * </ul>
 *
 * Main responsibilities include:
 * <ul>
 *   <li>Loading metadata records from disk</li>
 *   <li>Saving updated metadata lists</li>
 *   <li>Adding new file records</li>
 *   <li>Listing all files for a user</li>
 *   <li>Finding files by filename</li>
 *   <li>Deleting metadata records</li>
 * </ul>
 *
 * This class uses static methods only and serves as a centralized metadata
 * persistence service for the server.
 */
public class MetadataStore {

    /**
     * @brief Shared JSON object mapper for metadata serialization.
     *
     * @details
     * Used to convert JSON files into Java objects and vice versa.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * @brief Timestamp formatter used for upload dates.
     *
     * @details
     * Produces timestamps in the format {@code yyyy-MM-dd HH:mm:ss}.
     */
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * @brief Resolves the metadata file path for a specific user.
     *
     * @details
     * Returns the location of the user's {@code metadata.json} file inside the
     * server storage structure.
     *
     * @param userId The user identifier.
     *
     * @return Path to the user's metadata file.
     */
    private static Path userFile(String userId) {
        return Paths.get("server-data", userId, "metadata.json");
    }

    /**
     * @brief Loads all metadata records for a user.
     *
     * @details
     * Reads the user's {@code metadata.json} file and converts it into a list
     * of {@link FileMetadata} objects.
     *
     * If the file does not exist, an empty list is returned.
     *
     * If loading fails, an error is logged and an empty list is returned.
     *
     * @param userId The user whose metadata should be loaded.
     *
     * @return A list of file metadata records for the user.
     */
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

    /**
     * @brief Saves a list of metadata records for a user.
     *
     * @details
     * Writes the provided metadata collection to the user's
     * {@code metadata.json} file using formatted JSON output.
     *
     * If the parent directory does not exist, it is created automatically.
     *
     * If saving fails, an error message is written to the server log.
     *
     * @param userId The user whose metadata file should be updated.
     * @param records The metadata records to persist.
     */
    static void save(String userId, List<FileMetadata> records) {
        try {
            Path path = userFile(userId);
            Files.createDirectories(path.getParent());

            MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), records);
        } catch (Exception e) {
            Logger.error("Failed to save metadata for " + userId + ": " + e.getMessage());
        }
    }

    // ---------------- PUBLIC API ----------------

    /**
     * @brief Finds a metadata record by filename.
     *
     * @details
     * Searches the user's metadata list and returns the first file whose
     * filename exactly matches the provided name.
     *
     * @param userId The user who owns the file records.
     * @param filename The filename to search for.
     *
     * @return The matching {@link FileMetadata} record, or {@code null} if not found.
     */
    public static FileMetadata findByFilename(String userId, String filename) {
        return load(userId).stream()
                .filter(f -> f.getFilename().equals(filename))
                .findFirst()
                .orElse(null);
    }

    /**
     * @brief Adds a new file metadata record for a user.
     *
     * @details
     * Creates a new {@link FileMetadata} entry using the supplied values and
     * the current timestamp, appends it to the user's metadata list, and saves
     * the updated collection back to disk.
     *
     * A log entry is generated after successful insertion.
     *
     * @param userId The owner of the uploaded file.
     * @param fileId The internal unique file identifier.
     * @param filename The original filename.
     * @param size File size in bytes.
     * @param mime MIME type of the file.
     * @param additionalInfo Additional descriptive information supplied by the user.
     */
    public static void addFile(String userId,
                               String fileId,
                               String filename,
                               long size,
                               String mime,
                               String additionalInfo){

        List<FileMetadata> records = load(userId);

        FileMetadata record = new FileMetadata(
                fileId,
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

    /**
     * @brief Returns all metadata records for a user.
     *
     * @details
     * Loads and returns the full list of files associated with the specified user.
     *
     * @param userId The user whose files should be listed.
     *
     * @return A list of {@link FileMetadata} objects.
     */
    public static List<FileMetadata> listFiles(String userId) {
        return load(userId);
    }

    /**
     * @brief Retrieves a single file metadata record by filename.
     *
     * @details
     * Searches the user's metadata list and returns the matching file record.
     *
     * This method is functionally similar to {@link #findByFilename(String, String)}.
     *
     * @param userId The owner of the file.
     * @param filename The filename to locate.
     *
     * @return The matching {@link FileMetadata}, or {@code null} if not found.
     */
    public static FileMetadata getFile(String userId, String filename) {
        return load(userId).stream()
                .filter(f -> f.getFilename().equals(filename))
                .findFirst()
                .orElse(null);
    }

    /**
     * @brief Deletes a metadata record for a specific file.
     *
     * @details
     * Removes all records whose filename matches the provided name, saves the
     * updated metadata list, and logs the deletion event.
     *
     * This operation removes only metadata information. Physical file deletion
     * must be handled separately by the file storage service.
     *
     * @param userId The owner of the file record.
     * @param filename The filename whose metadata should be removed.
     */
    public static void deleteFile(String userId, String filename) {
        List<FileMetadata> records = load(userId);
        records.removeIf(f -> f.getFilename().equals(filename));
        save(userId, records);

        Logger.info("Metadata deleted for file: " + filename);
    }
}