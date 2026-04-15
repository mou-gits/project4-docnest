package ca.docnest.util;

import ca.docnest.shared.model.FileMetadata;
import ca.docnest.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @class JsonUtils
 * @brief Provides utility methods for reading and writing JSON data files.
 *
 * @details
 * The {@code JsonUtils} class is a helper utility responsible for loading and
 * saving structured JSON data used by the DocNest application.
 *
 * It uses the Jackson {@link ObjectMapper} library to convert between JSON
 * files and Java objects.
 *
 * Supported operations include:
 * <ul>
 *   <li>Loading user accounts from {@code users.json}</li>
 *   <li>Loading file metadata from a user's folder</li>
 *   <li>Saving file metadata back to disk</li>
 * </ul>
 *
 * This class contains only static methods and is intended to be used without
 * instantiation.
 *
 * Error handling is intentionally simple in this implementation:
 * exceptions are printed to the console and fallback empty collections are
 * returned when loading fails.
 */
public class JsonUtils {

    /**
     * @brief Shared Jackson object mapper used for JSON serialization.
     *
     * @details
     * This mapper converts JSON content into Java objects and converts Java
     * objects back into JSON format when saving data.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * @brief Loads user account data from the application resources folder.
     *
     * @details
     * This method reads the file {@code users/users.json} from the application
     * classpath resources and converts it into a list of {@link User} objects.
     *
     * The JSON file is expected to contain an array of user records.
     *
     * If loading fails for any reason (missing file, invalid JSON, I/O error),
     * the exception stack trace is printed and an empty immutable list is
     * returned.
     *
     * @return A list of {@link User} objects loaded from the JSON file.
     *         Returns an empty list if loading fails.
     */
    public static List<User> loadUsers() {
        try (InputStream is = JsonUtils.class.getClassLoader()
                .getResourceAsStream("users/users.json")) {

            return mapper.readValue(is, new TypeReference<List<User>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * @brief Loads file metadata from a user's folder.
     *
     * @details
     * This method searches for a file named {@code metadata.json} inside the
     * specified user folder.
     *
     * If the file exists, it is parsed into a list of
     * {@link FileMetadata} objects.
     *
     * If the file does not exist, an empty list is returned.
     *
     * If parsing or file access fails, the exception stack trace is printed
     * and an empty list is returned.
     *
     * @param userFolder The folder belonging to a specific user that may
     *                   contain the metadata file.
     *
     * @return A list of {@link FileMetadata} records associated with the user.
     *         Returns an empty list if no metadata exists or loading fails.
     */
    public static List<FileMetadata> loadMetadata(File userFolder) {
        try {
            File metaFile = new File(userFolder, "metadata.json");
            if (!metaFile.exists()) {
                return new ArrayList<>();
            }

            return mapper.readValue(metaFile, new TypeReference<List<FileMetadata>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * @brief Saves file metadata to a user's folder as JSON.
     *
     * @details
     * This method writes the provided list of {@link FileMetadata} objects to
     * a file named {@code metadata.json} inside the specified user folder.
     *
     * The JSON output is formatted using Jackson's default pretty printer for
     * improved readability.
     *
     * If saving fails, the exception stack trace is printed to the console.
     *
     * @param userFolder The folder where the metadata file should be written.
     * @param list The list of file metadata objects to save.
     */
    public static void saveMetadata(File userFolder, List<FileMetadata> list) {
        try {
            File metaFile = new File(userFolder, "metadata.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(metaFile, list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}