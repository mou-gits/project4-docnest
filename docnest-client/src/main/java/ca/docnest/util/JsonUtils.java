package ca.docnest.util;

import ca.docnest.shared.model.FileMetadata;
import ca.docnest.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Load users.json from resources
    public static List<User> loadUsers() {
        try (InputStream is = JsonUtils.class.getClassLoader()
                .getResourceAsStream("users/users.json")) {

            return mapper.readValue(is, new TypeReference<List<User>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // Load metadata.json from user folder
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

    // Save metadata.json to user folder
    public static void saveMetadata(File userFolder, List<FileMetadata> list) {
        try {
            File metaFile = new File(userFolder, "metadata.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(metaFile, list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
