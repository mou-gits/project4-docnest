package ca.docnest.shared.protocol;

import org.junit.jupiter.api.Test;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    @Test
    void testInvalidCommandId() {
        byte[] bad = {
                0,0,0,0,   // invalid command
                0,0,0,5    // payload size
        };
        ByteArrayInputStream in = new ByteArrayInputStream(bad);

        assertThrows(IOException.class, () -> DataPacket.readFrom(in));
    }
}
