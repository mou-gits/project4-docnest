package ca.docnest.ui;

import ca.docnest.model.FileMetadata;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class FileInfoDialogFX {

    public static void show(FileMetadata meta) {
        if (meta == null) return;

        String message = """
                File Name: %s
                Owner: %s
                Size: %d bytes
                Type: %s
                Upload Date: %s
                Additional Info: %s
                """.formatted(
                meta.getFilename(),
                meta.getUploadedBy(),
                meta.getSize(),
                meta.getType(),
                meta.getUploadDate(),
                meta.getInfo()
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText("File Info");
        alert.showAndWait();
    }
}
