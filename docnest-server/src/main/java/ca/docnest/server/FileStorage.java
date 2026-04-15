package ca.docnest.server;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @class FileStorage
 * @brief Provides server-side file storage management for user uploads and downloads.
 *
 * @details
 * The {@code FileStorage} class is responsible for managing physical file
 * storage on the DocNest server. It supports upload initialization, chunk
 * appending, upload completion, file reading, and file deletion.
 *
 * Files are stored under a root directory named {@code server-data}, with a
 * separate subdirectory for each user. Uploads are handled using a temporary
 * file mechanism to ensure that partially uploaded files are not treated as
 * completed files.
 *
 * Main responsibilities include:
 * <ul>
 *   <li>Creating and managing per-user storage directories</li>
 *   <li>Tracking active uploads in memory</li>
 *   <li>Appending incoming upload chunks to temporary files</li>
 *   <li>Finalizing completed uploads into permanent binary files</li>
 *   <li>Reading stored files by identifier or filename</li>
 *   <li>Deleting files by identifier or filename</li>
 *   <li>Applying simple sanitization to user and file names</li>
 * </ul>
 *
 * This class uses only static methods and is intended to act as a shared
 * storage service for the server application.
 */
public class FileStorage {

    /**
     * @brief Root directory for all stored user data.
     *
     * @details
     * Each user receives a sanitized subdirectory under this root path.
     */
    private static final Path ROOT = Paths.get("server-data");

    /**
     * @brief Tracks one active upload context per user.
     *
     * @details
     * This map stores upload progress information for each user currently
     * performing an upload. A {@link ConcurrentHashMap} is used to support
     * concurrent access across multiple client sessions.
     */
    private static final Map<String, UploadContext> activeUploads = new ConcurrentHashMap<>();

    /**
     * @brief Static initialization block that ensures the storage root exists.
     *
     * @details
     * Creates the {@code server-data} root directory when the class is first
     * loaded. If the directory cannot be created, a runtime exception is thrown
     * because the server cannot function without storage.
     */
    static {
        try {
            Files.createDirectories(ROOT);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage root: " + ROOT, e);
        }
    }

    // ---------- Public API ----------

    /**
     * @brief Begins a new upload session for a user.
     *
     * @details
     * This method prepares the server to receive a file upload by:
     * <ol>
     *   <li>Ensuring the user's storage directory exists</li>
     *   <li>Sanitizing the provided filename</li>
     *   <li>Creating or replacing a temporary {@code .part} file</li>
     *   <li>Registering an {@link UploadContext} in the active uploads map</li>
     * </ol>
     *
     * The file is not finalized at this stage. Chunks must still be appended,
     * and {@link #finishUpload(String)} must be called later.
     *
     * @param userId The ID of the user performing the upload.
     * @param fileId The unique internal file identifier assigned to the upload.
     * @param filename The original name of the file being uploaded.
     * @param size The expected total size of the file in bytes.
     *
     * @throws IOException If the user directory or temporary file cannot be
     *                     created or replaced.
     */
    public static void beginUpload(String userId, String fileId, String filename, long size) throws IOException {
        Path userDir = userDir(userId);
        Files.createDirectories(userDir);

        String safeName = sanitizeFilename(filename);

        Path tempFile = userDir.resolve(fileId + ".part");

        // Overwrite any existing temp file
        if (Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        Files.createFile(tempFile);

        UploadContext ctx = new UploadContext(userId, fileId, filename, size, tempFile);
        activeUploads.put(userId, ctx);
    }

    /**
     * @brief Appends a received data chunk to the active upload for a user.
     *
     * @details
     * This method retrieves the active upload context for the specified user,
     * appends the provided byte chunk to the temporary upload file, and updates
     * the count of bytes received.
     *
     * If the total bytes received exceed the expected file size, an exception
     * is thrown to indicate a protocol or data transfer problem.
     *
     * @param userId The ID of the user whose upload is in progress.
     * @param chunk The byte array chunk to append to the temporary file.
     *
     * @throws IOException If no active upload exists, if file writing fails, or
     *                     if more bytes are received than expected.
     */
    public static void appendChunk(String userId, byte[] chunk) throws IOException {
        UploadContext ctx = activeUploads.get(userId);
        if (ctx == null) {
            throw new IOException("No active upload for user: " + userId);
        }

        Files.write(ctx.tempFile, chunk, StandardOpenOption.APPEND);
        ctx.bytesReceived += chunk.length;

        if (ctx.bytesReceived > ctx.expectedSize) {
            throw new IOException("Received more data than expected for " + ctx.filename);
        }
    }

    /**
     * @brief Finalizes the active upload for a user.
     *
     * @details
     * This method completes the upload process by validating that the number of
     * bytes received matches the expected file size.
     *
     * If the upload is complete:
     * <ul>
     *   <li>The temporary {@code .part} file is renamed to a final
     *       {@code .bin} file</li>
     *   <li>The upload context is removed from the active uploads map</li>
     * </ul>
     *
     * If the upload is incomplete:
     * <ul>
     *   <li>The temporary file is deleted if possible</li>
     *   <li>An exception is thrown</li>
     * </ul>
     *
     * @param userId The ID of the user whose upload is being finalized.
     *
     * @throws IOException If no upload is active, if the byte count is wrong,
     *                     or if the temporary file cannot be moved.
     */
    public static void finishUpload(String userId) throws IOException {
        UploadContext ctx = activeUploads.remove(userId);
        if (ctx == null) {
            throw new IOException("No active upload for user: " + userId);
        }
        if (ctx.bytesReceived != ctx.expectedSize) {

            // Cleanup temp file
            try {
                Files.deleteIfExists(ctx.tempFile);
            } catch (IOException ignored) {}

            throw new IOException(
                    "Upload incomplete: expected " + ctx.expectedSize +
                            " bytes but received " + ctx.bytesReceived
            );
        }

        Path userDir = userDir(userId);
        String safeName = sanitizeFilename(ctx.filename);
        Path finalFile = userDir.resolve(ctx.fileId + ".bin");

        Files.move(ctx.tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * @brief Reads a stored file for a user by filename.
     *
     * @details
     * This method sanitizes the given filename, resolves it relative to the
     * user's storage directory, checks for existence, and returns the file
     * contents as a byte array.
     *
     * @param userId The ID of the user who owns the file.
     * @param filename The filename to read.
     *
     * @return A byte array containing the full file contents.
     *
     * @throws IOException If the file does not exist or cannot be read.
     */
    public static byte[] readFile(String userId, String filename) throws IOException {
        String safeName = sanitizeFilename(filename);
        Path file = userDir(userId).resolve(safeName);
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + filename);
        }
        return Files.readAllBytes(file);
    }

    /**
     * @brief Deletes a stored file for a user by filename.
     *
     * @details
     * This method sanitizes the filename, resolves it within the user's
     * directory, and deletes the file if it exists.
     *
     * @param userId The ID of the user who owns the file.
     * @param filename The filename of the file to delete.
     *
     * @return {@code true} if the file existed and was deleted;
     *         {@code false} if the file did not exist.
     *
     * @throws IOException If the file exists but cannot be deleted.
     */
    public static boolean deleteFile(String userId, String filename) throws IOException {
        String safeName = sanitizeFilename(filename);
        Path file = userDir(userId).resolve(safeName);
        if (!Files.exists(file)) {
            return false;
        }
        Files.delete(file);
        return true;
    }

    // ---------- Helpers ----------

    /**
     * @brief Deletes a stored file for a user by internal file ID.
     *
     * @details
     * Resolves the file path using the given file ID with a {@code .bin}
     * extension, logs diagnostic information, and deletes the file if found.
     *
     * @param userId The ID of the user who owns the file.
     * @param fileId The internal file identifier of the file to delete.
     *
     * @return {@code true} if the file existed and was deleted;
     *         {@code false} otherwise.
     *
     * @throws IOException If the file exists but deletion fails.
     */
    public static boolean deleteFileById(String userId, String fileId) throws IOException {
        Path file = userDir(userId).resolve(fileId + ".bin");

        Logger.info("DELETE path: " + file.toAbsolutePath());
        Logger.info("Exists? " + Files.exists(file));

        if (!Files.exists(file)) {
            return false;
        }

        Files.delete(file);
        return true;
    }

    /**
     * @brief Reads a stored file for a user by internal file ID.
     *
     * @details
     * Resolves the file path using the given file ID with a {@code .bin}
     * extension and returns the file contents as bytes.
     *
     * @param userId The ID of the user who owns the file.
     * @param fileId The internal file identifier of the file to read.
     *
     * @return A byte array containing the file data.
     *
     * @throws IOException If the file does not exist or cannot be read.
     */
    public static byte[] readFileById(String userId, String fileId) throws IOException {
        Path file = userDir(userId).resolve(fileId + ".bin");

        if (!Files.exists(file)) {
            throw new IOException("File not found: " + fileId);
        }

        return Files.readAllBytes(file);
    }

    /**
     * @brief Returns the storage directory path for a specific user.
     *
     * @details
     * The user ID is sanitized before being used as a directory name.
     *
     * @param userId The user identifier.
     *
     * @return The resolved user directory path under the storage root.
     */
    private static Path userDir(String userId) {
        return ROOT.resolve(sanitize(userId));
    }

    /**
     * @brief Sanitizes a general string for safe filesystem usage.
     *
     * @details
     * Replaces any character not matching letters, digits, dot, underscore, or
     * hyphen with an underscore to reduce path traversal and invalid path risks.
     *
     * @param s The input string to sanitize.
     *
     * @return A sanitized string safe for path construction.
     */
    private static String sanitize(String s) {
        // Very simple sanitization to avoid path traversal
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * @brief Sanitizes a filename for safe storage.
     *
     * @details
     * This method validates that the filename is non-null and non-blank,
     * removes any path components, and replaces unsafe characters with
     * underscores.
     *
     * @param filename The original filename provided by the client.
     *
     * @return A sanitized filename safe for filesystem use.
     *
     * @throws IllegalArgumentException If the filename is null or blank.
     */
    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Remove any path components (e.g., ../../ or folders)
        filename = Paths.get(filename).getFileName().toString();

        // Replace unsafe characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ---------- Internal Types ----------

    /**
     * @class UploadContext
     * @brief Stores temporary state for an active file upload.
     *
     * @details
     * This internal helper class tracks metadata and progress information for
     * a file currently being uploaded by a specific user.
     *
     * It stores:
     * <ul>
     *   <li>The user ID performing the upload</li>
     *   <li>The internal file ID</li>
     *   <li>The original filename</li>
     *   <li>The expected total size</li>
     *   <li>The temporary file path</li>
     *   <li>The number of bytes received so far</li>
     * </ul>
     */
    private static class UploadContext {

        /**
         * @brief ID of the user performing the upload.
         */
        final String userId;

        /**
         * @brief Internal unique identifier assigned to the uploaded file.
         */
        final String fileId;

        /**
         * @brief Original filename supplied by the client.
         */
        final String filename;

        /**
         * @brief Expected final size of the uploaded file in bytes.
         */
        final long expectedSize;

        /**
         * @brief Path to the temporary partial file on disk.
         */
        final Path tempFile;

        /**
         * @brief Number of bytes received so far for this upload.
         *
         * @details
         * This value starts at zero and increases as chunks are appended.
         */
        long bytesReceived = 0;

        /**
         * @brief Constructs a new upload context.
         *
         * @param userId The uploading user's ID.
         * @param fileId The internal file identifier.
         * @param filename The original filename.
         * @param expectedSize The expected size of the full file in bytes.
         * @param tempFile The temporary file path used during upload.
         */
        UploadContext(String userId,
                      String fileId,
                      String filename,
                      long expectedSize,
                      Path tempFile) {

            this.userId = userId;
            this.fileId = fileId;
            this.filename = filename;
            this.expectedSize = expectedSize;
            this.tempFile = tempFile;
        }
    }
}