package ca.docnest.ui;

import ca.docnest.shared.model.FileMetadata;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * @class FileInfoDialogFX
 * @brief Displays file metadata information in a JavaFX dialog window.
 *
 * @details
 * The {@code FileInfoDialogFX} class is a lightweight utility class used to
 * present details about a selected file to the user through a JavaFX
 * information alert.
 *
 * It receives a {@link FileMetadata} object and formats its contents into a
 * readable message that includes:
 * <ul>
 *   <li>File name</li>
 *   <li>Owner / uploader</li>
 *   <li>File size in bytes</li>
 *   <li>File type</li>
 *   <li>Upload date</li>
 *   <li>Additional descriptive information</li>
 * </ul>
 *
 * The dialog is modal and waits for the user to acknowledge it by pressing
 * the OK button.
 *
 * This class contains only static behavior and does not need to be
 * instantiated.
 */
public class FileInfoDialogFX {

    /**
     * @brief Displays a modal dialog containing file metadata information.
     *
     * @details
     * This method creates a JavaFX {@link Alert} of type
     * {@code INFORMATION}, formats the supplied {@link FileMetadata} object
     * into a multi-line message, and shows the dialog to the user.
     *
     * If the provided metadata object is {@code null}, the method exits
     * immediately and no dialog is shown.
     *
     * The displayed message includes:
     * <ul>
     *   <li>File Name</li>
     *   <li>Owner</li>
     *   <li>Size</li>
     *   <li>Type</li>
     *   <li>Upload Date</li>
     *   <li>Additional Info</li>
     * </ul>
     *
     * The dialog remains visible until the user clicks the OK button.
     *
     * @param meta The {@link FileMetadata} object containing the file details
     *             to display. If {@code null}, no action is taken.
     */
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
                meta.getAdditionalInfo()
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText("File Info");
        alert.showAndWait();
    }
}