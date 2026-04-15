package ca.docnest.model;

/**
 * @class User
 * @brief Represents a user entity within the DocNest system.
 *
 * @details
 * The {@code User} class is a simple data model used to store and transfer
 * user-related information within the application.
 *
 * It encapsulates the core identity and authentication details associated
 * with a system user:
 * <ul>
 *   <li>User identifier</li>
 *   <li>User display name</li>
 *   <li>User password</li>
 * </ul>
 *
 * This class may be used in authentication workflows, user management
 * operations, data persistence, and session handling.
 *
 * It follows a standard JavaBean-style structure with private fields,
 * constructors, and getter methods for controlled access to data.
 *
 * @note
 * In production systems, passwords should never be stored in plain text.
 * They should be securely hashed and salted before storage.
 */
public class User {

    /**
     * @brief Unique identifier assigned to the user.
     *
     * @details
     * This field stores the internal user ID used to uniquely distinguish
     * one user from another in the system.
     */
    private String userId;

    /**
     * @brief Human-readable name of the user.
     *
     * @details
     * This field stores the user's display name or full name, which may be
     * shown in the user interface or reports.
     */
    private String name;

    /**
     * @brief Password associated with the user account.
     *
     * @details
     * This field stores the user's password for authentication purposes.
     *
     * @warning
     * Storing passwords as plain text is insecure and should only be used
     * in educational or prototype environments.
     */
    private String password;

    /**
     * @brief Default constructor for the User class.
     *
     * @details
     * Creates an empty {@code User} object with all fields initialized to
     * their default value of {@code null}.
     *
     * This constructor is useful for frameworks, serialization libraries,
     * or cases where values will be assigned later.
     */
    public User() {}

    /**
     * @brief Constructs a User object with all fields initialized.
     *
     * @details
     * Creates a fully populated user instance using the provided user ID,
     * name, and password values.
     *
     * @param userId The unique identifier for the user.
     * @param name The display or full name of the user.
     * @param password The password associated with the user account.
     */
    public User(String userId, String name, String password) {
        this.userId = userId;
        this.name = name;
        this.password = password;
    }

    /**
     * @brief Returns the user's unique identifier.
     *
     * @details
     * Retrieves the internal ID assigned to this user.
     *
     * @return The user ID as a {@link String}.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @brief Returns the user's name.
     *
     * @details
     * Retrieves the display name or full name associated with this user.
     *
     * @return The user's name as a {@link String}.
     */
    public String getName() {
        return name;
    }

    /**
     * @brief Returns the user's password.
     *
     * @details
     * Retrieves the stored password value for this user.
     *
     * @return The user's password as a {@link String}.
     *
     * @warning
     * Exposing raw passwords is insecure in real-world systems.
     */
    public String getPassword() {
        return password;
    }
}