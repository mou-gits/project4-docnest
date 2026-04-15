package ca.docnest.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

/**
 * @class AuthService
 * @brief Provides user authentication services for the DocNest server.
 *
 * @details
 * The {@code AuthService} class is a utility-style server component responsible
 * for validating login credentials against user records stored in the
 * {@code users.json} resource file.
 *
 * User data is loaded once during class initialization and cached in memory
 * for efficient authentication checks during runtime.
 *
 * This class is declared {@code final} to prevent inheritance and uses a
 * private constructor to prevent instantiation.
 *
 * Main responsibilities include:
 * <ul>
 *   <li>Loading user records from JSON configuration data</li>
 *   <li>Authenticating username and password combinations</li>
 *   <li>Providing centralized credential validation for the server</li>
 * </ul>
 *
 * If the user data cannot be loaded during startup, the class throws an
 * {@link IllegalStateException}, preventing the server from running with an
 * invalid authentication state.
 */
public final class AuthService {

    /**
     * @brief Shared JSON object mapper used to parse user records.
     *
     * @details
     * This mapper converts the contents of {@code users.json} into Java record
     * objects used internally by the authentication service.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * @brief Cached list of user records loaded at class initialization.
     *
     * @details
     * This collection contains all valid users loaded from the JSON resource.
     * It is initialized once by calling {@link #loadUsers()} and reused for
     * all authentication requests.
     */
    private static final List<UserRecord> USERS = loadUsers();

    /**
     * @brief Private constructor to prevent instantiation.
     *
     * @details
     * This class is designed as a static utility service and should not be
     * instantiated.
     */
    private AuthService() {
    }

    /**
     * @brief Validates a username and password combination.
     *
     * @details
     * This method checks whether the provided credentials match any loaded user
     * record stored in memory.
     *
     * Authentication succeeds only when:
     * <ul>
     *   <li>The username matches a stored {@code userId}</li>
     *   <li>The password matches the corresponding stored password</li>
     * </ul>
     *
     * The search is performed using the Java Stream API.
     *
     * @param username The username supplied by the client.
     * @param password The password supplied by the client.
     *
     * @return {@code true} if a matching user exists; {@code false} otherwise.
     */
    public static boolean authenticate(String username, String password) {
        return USERS.stream()
                .anyMatch(user -> user.userId().equals(username) && user.password().equals(password));
    }

    /**
     * @brief Loads user records from the server classpath resource file.
     *
     * @details
     * This method reads the JSON file located at
     * {@code users/users.json} from the server application's classpath and
     * converts it into a list of {@link UserRecord} entries.
     *
     * If the file cannot be found, an exception is thrown immediately.
     *
     * If parsing fails or any other error occurs, the exception is wrapped in
     * an {@link IllegalStateException}.
     *
     * This method is intended to run once during static initialization.
     *
     * @return A list of user records loaded from the JSON file.
     *
     * @throws IllegalStateException If the resource file is missing or cannot
     *                               be parsed successfully.
     */
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

    /**
     * @brief Internal immutable record representing a user account.
     *
     * @details
     * This record stores the fields required for authentication and user
     * identity:
     * <ul>
     *   <li>User ID</li>
     *   <li>Name</li>
     *   <li>Password</li>
     * </ul>
     *
     * It is used only within {@link AuthService} as a lightweight data holder
     * for parsed JSON user entries.
     *
     * @param userId Unique login identifier for the user.
     * @param name Display name of the user.
     * @param password Password associated with the account.
     */
    private record UserRecord(String userId, String name, String password) {
    }
}