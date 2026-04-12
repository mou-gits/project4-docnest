package ca.docnest.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple file storage for DocNest server.
 * Per-user directories, chunked uploads, full-file reads.
 */
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

    public static void beginUpload(String userId, String filename, long size) throws IOException {
        Path userDir = userDir(userId);
        Files.createDirectories(userDir);

        Path tempFile = userDir.resolve(filename + ".part");

        // Overwrite any existing temp file
        if (Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        Files.createFile(tempFile);

        UploadContext ctx = new UploadContext(userId, filename, size, tempFile);
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

        Path userDir = userDir(userId);
        Path finalFile = userDir.resolve(ctx.filename);

        // Move temp file to final location
        Files.move(ctx.tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public static List<String> listFilesForUser(String userId) throws IOException {
        Path userDir = userDir(userId);
        if (!Files.exists(userDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(userDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".part"))
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        }
    }

    public static byte[] readFile(String userId, String filename) throws IOException {
        Path file = userDir(userId).resolve(filename);
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + filename);
        }
        return Files.readAllBytes(file);
    }

    public static byte[] readChunk(String userId, String filename, int offset, int length) throws IOException {
        Path file = userDir(userId).resolve(filename);
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + filename);
        }

        try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            if (offset >= channel.size()) {
                return new byte[0];
            }

            channel.position(offset);
            ByteBuffer buffer = ByteBuffer.allocate(length);
            int read = channel.read(buffer);
            if (read <= 0) {
                return new byte[0];
            }

            byte[] data = new byte[read];
            buffer.flip();
            buffer.get(data);
            return data;
        }
    }

    public static boolean deleteFile(String userId, String filename) throws IOException {
        Path file = userDir(userId).resolve(filename);
        if (!Files.exists(file)) {
            return false;
        }
        Files.delete(file);
        return true;
    }

    // ---------- Helpers ----------

    private static Path userDir(String userId) {
        return ROOT.resolve(sanitize(userId));
    }

    private static String sanitize(String s) {
        // Very simple sanitization to avoid path traversal
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ---------- Internal Types ----------

    private static class UploadContext {
        final String userId;
        final String filename;
        final long expectedSize;
        final Path tempFile;
        long bytesReceived = 0;

        UploadContext(String userId, String filename, long expectedSize, Path tempFile) {
            this.userId = userId;
            this.filename = filename;
            this.expectedSize = expectedSize;
            this.tempFile = tempFile;
        }
    }
}
