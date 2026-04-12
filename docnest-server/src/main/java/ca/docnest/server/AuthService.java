package ca.docnest.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

public final class AuthService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<UserRecord> USERS = loadUsers();

    private AuthService() {
    }

    public static boolean authenticate(String username, String password) {
        return USERS.stream()
                .anyMatch(user -> user.userId().equals(username) && user.password().equals(password));
    }

    private static List<UserRecord> loadUsers() {
        try (InputStream in = AuthService.class.getClassLoader().getResourceAsStream("users/users.json")) {
            if (in == null) {
                throw new IllegalStateException("users/users.json was not found on the server classpath");
            }
            return MAPPER.readValue(in, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load users.json", e);
        }
    }

    private record UserRecord(String userId, String name, String password) {
    }
}
