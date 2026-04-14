package ca.docnest.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @Test
    void testValidLogin() {
        assertTrue(AuthService.authenticate("admin", "1234"));
    }

    @Test
    void testInvalidLogin() {
        assertFalse(AuthService.authenticate("mou", "wrong"));
    }
}