package ca.docnest.server;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class FileStorageTest {

    private static final String USER = "testuser";

    @BeforeEach
    void setup() throws Exception {
        // Clean test directory
        Path dir = Paths.get("server-data", USER);
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted((a,b) -> b.compareTo(a))
                    .forEach(p -> p.toFile().delete());
        }
    }

    @Test
    void testUploadAndDelete() throws Exception {

        String fileId = "test-id";
        String filename = "test.txt";

        // Begin upload
        FileStorage.beginUpload(USER, fileId, filename, 5);

        // Write chunk
        FileStorage.appendChunk(USER, "hello".getBytes());

        // Finish upload
        FileStorage.finishUpload(USER);

        // Verify file exists
        Path file = Paths.get("server-data", USER, fileId + ".bin");
        assertTrue(Files.exists(file));

        // Delete file
        boolean deleted = FileStorage.deleteFileById(USER, fileId);
        assertTrue(deleted);

        assertFalse(Files.exists(file));
    }
}