package ca.docnest.server;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/* File storage for server*/
public class FileStorage {
    // Root directory for all user data
    private static final Path ROOT = Paths.get("server-data");

    // One active upload per user (simple model)
    private static final Map<String, UploadContext> activeUploads = new ConcurrentHashMap<>();

    static {
        try {
            Files.createDirectories(ROOT);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage root: " + ROOT, e);
        }
    }

    // ---------- Public API ----------

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

    public static byte[] readFile(String userId, String filename) throws IOException {
        String safeName = sanitizeFilename(filename);
        Path file = userDir(userId).resolve(safeName);
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + filename);
        }
        return Files.readAllBytes(file);
    }

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

    public static byte[] readFileById(String userId, String fileId) throws IOException {
        Path file = userDir(userId).resolve(fileId + ".bin");

        if (!Files.exists(file)) {
            throw new IOException("File not found: " + fileId);
        }

        return Files.readAllBytes(file);
    }

    private static Path userDir(String userId) {
        return ROOT.resolve(sanitize(userId));
    }

    private static String sanitize(String s) {
        // Very simple sanitization to avoid path traversal
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

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

    private static class UploadContext {
        final String userId;
        final String fileId;
        final String filename;
        final long expectedSize;
        final Path tempFile;
        long bytesReceived = 0;

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
