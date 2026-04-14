package ca.docnest.server;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MetadataStoreTest {

    private static final String USER = "testuser";

    @BeforeEach
    void setup() {
        MetadataStore.save(USER, new java.util.ArrayList<>());
    }

    @Test
    void testAddAndFind() {

        MetadataStore.addFile(
                USER,
                "id-123",
                "file.txt",
                100,
                "text/plain",
                "info"
        );

        var meta = MetadataStore.findByFilename(USER, "file.txt");

        assertNotNull(meta);
        assertEquals("id-123", meta.getFileId());
    }
}