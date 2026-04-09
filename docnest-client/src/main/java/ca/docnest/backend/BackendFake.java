package ca.docnest.backend;

import ca.docnest.model.FileMetadata;
import ca.docnest.model.User;
import ca.docnest.util.JsonUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;

public class BackendFake {

    private final List<User> users;
    private final File storageRoot;

    public BackendFake() {
        // Load users from resources
        this.users = JsonUtils.loadUsers();

        // Resolve storage folder from resources
        this.storageRoot = resolveStorageRoot();
    }

    public User login(String username, String password) {
        return users.stream()
                .filter(u -> u.getUserId().equals(username) && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    public List<FileMetadata> listFiles(String userId) {
        File userFolder = getUserFolder(userId);
        return JsonUtils.loadMetadata(userFolder);
    }

    public byte[] downloadFile(String userId, String filename) {
        try {
            File userFolder = getUserFolder(userId);
            File file = new File(userFolder, filename);
            return Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteFile(String userId, String filename) {
        try {
            File userFolder = getUserFolder(userId);
            File file = new File(userFolder, filename);

            if (file.exists()) {
                file.delete();
            }

            // Update metadata
            List<FileMetadata> list = JsonUtils.loadMetadata(userFolder);
            list.removeIf(m -> m.getFilename().equals(filename));
            JsonUtils.saveMetadata(userFolder, list);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean uploadFile(String userId, File selectedFile, String info) {
        try {
            File userFolder = getUserFolder(userId);

            // Copy file into user folder
            File dest = new File(userFolder, selectedFile.getName());
            Files.copy(selectedFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Build metadata
            String type = Files.probeContentType(selectedFile.toPath());
            if (type == null) type = "application/octet-stream";

            FileMetadata meta = new FileMetadata(
                    selectedFile.getName(),
                    selectedFile.length(),
                    type,
                    userId,
                    LocalDate.now().toString(),
                    info
            );

            // Load existing metadata
            List<FileMetadata> list = JsonUtils.loadMetadata(userFolder);
            list.add(meta);

            // Save metadata
            JsonUtils.saveMetadata(userFolder, list);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private File resolveStorageRoot() {
        try {
            // Cross-platform user home directory
            String home = System.getProperty("user.home");

            // Create a dedicated folder for DocNest
            File folder = new File(home, "DocNestStorage");

            if (!folder.exists()) {
                folder.mkdirs();
            }

            return folder;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve storage folder", e);
        }
    }


    private File getUserFolder(String userId) {
        File userFolder = new File(storageRoot, userId);
        if (!userFolder.exists()) {
            userFolder.mkdirs();
        }
        return userFolder;
    }
}
