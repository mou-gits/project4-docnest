package ca.docnest.ui;

import ca.docnest.client.network.ClientNetwork;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

/**
 * @class UploadDialogFX
 * @brief Provides a JavaFX modal dialog for uploading files to the DocNest server.
 *
 * @details
 * The {@code UploadDialogFX} class creates a graphical dialog window that allows
 * users to select a local file, optionally enter additional descriptive
 * information, and upload the file to the remote DocNest server.
 *
 * The dialog includes:
 * <ul>
 *   <li>A file browser for selecting a local file</li>
 *   <li>A text field for entering additional information</li>
 *   <li>A progress bar showing upload activity</li>
 *   <li>Error feedback for failed uploads</li>
 *   <li>Upload and Cancel controls</li>
 * </ul>
 *
 * The upload operation is executed in a background JavaFX {@link Task} so the
 * user interface remains responsive during file transfer.
 *
 * This dialog is modal and blocks interaction with the parent window until
 * closed.
 */
public class UploadDialogFX {

    /**
     * @brief Network communication service used to contact the server.
     *
     * @details
     * This client object is responsible for performing the actual upload
     * request through the DocNest communication protocol.
     */
    private final ClientNetwork client;

    /**
     * @brief Parent stage that owns this dialog.
     *
     * @details
     * If provided, the upload dialog is attached to this owner window and
     * behaves as its child modal dialog.
     */
    private final Stage owner;

    /**
     * @brief The file currently selected by the user for upload.
     *
     * @details
     * This field is assigned when the user chooses a file using the file
     * browser dialog. It remains {@code null} until a valid selection is made.
     */
    private File selectedFile;

    /**
     * @brief Constructs an upload dialog linked to a parent window and client.
     *
     * @details
     * Initializes the dialog with the specified owner stage and active network
     * client used for server communication.
     *
     * @param owner The parent {@link Stage} that owns this dialog. May be
     *              {@code null}.
     * @param client The active {@link ClientNetwork} instance used for uploads.
     */
    public UploadDialogFX(Stage owner, ClientNetwork client) {
        this.owner = owner;
        this.client = client;
    }

    /**
     * @brief Creates a background task that uploads a file to the server.
     *
     * @details
     * This helper method builds a JavaFX {@link Task} that performs the upload
     * process in a separate thread. The selected file is read into memory as a
     * byte array and sent to the server using the client network service.
     *
     * The task updates progress before and after the upload operation.
     *
     * @param file The local file to upload.
     * @param info Additional descriptive information entered by the user.
     *
     * @return A JavaFX {@link Task} representing the upload operation.
     */
    private Task<Void> createUploadTask(File file, String info) {
        return new Task<>() {

            /**
             * @brief Executes the upload operation in the background.
             *
             * @details
             * Reads the selected file from disk, sends it to the server using
             * the client upload method, and updates progress state.
             *
             * @return {@code null} when the upload completes successfully.
             *
             * @throws Exception Thrown if file reading fails or if the upload
             *                   process encounters communication errors.
             */
            @Override
            protected Void call() throws Exception {
                updateProgress(-1, 1);

                client.upload(
                        file.getName(),
                        java.nio.file.Files.readAllBytes(file.toPath()),
                        info
                );

                updateProgress(1, 1);
                return null;
            }
        };
    }

    /**
     * @brief Displays the upload dialog and waits until it is closed.
     *
     * @details
     * This method builds the full JavaFX user interface, wires all event
     * handlers, and displays the modal upload dialog.
     *
     * Main workflow:
     * <ol>
     *   <li>Create dialog window and controls</li>
     *   <li>Allow user to browse for a file</li>
     *   <li>Accept optional additional information</li>
     *   <li>Start background upload task when Upload is clicked</li>
     *   <li>Update progress bar during upload</li>
     *   <li>Close dialog on success</li>
     *   <li>Show error message on failure</li>
     * </ol>
     *
     * Cancel behavior:
     * <ul>
     *   <li>Attempts to close the client connection</li>
     *   <li>Closes the dialog window</li>
     * </ul>
     *
     * The method blocks until the user closes the dialog because it uses
     * {@code showAndWait()}.
     */
    public void showAndWait() {
        Stage stage = new Stage();
        stage.setTitle("Upload File");

        if (owner != null) {
            stage.initOwner(owner);
        }

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        FileChooser chooser = new FileChooser();

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        // ---- UI Elements ----
        Label lblFile = new Label("Choose File:");
        Label lblFileName = new Label("(none)");

        Label lblInfo = new Label("Additional Info:");
        TextField txtInfo = new TextField();
        txtInfo.setPrefWidth(250);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(250);

        Button btnChoose = new Button("Browse...");
        Button btnUpload = new Button("Upload");
        Button btnCancel = new Button("Cancel");

        btnUpload.setDisable(true);
        btnCancel.setDisable(false);

        HBox buttonBox = new HBox(10, btnCancel, btnUpload);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // ---- Layout ----
        grid.add(lblFile, 0, 0);
        grid.add(lblFileName, 1, 0);
        grid.add(btnChoose, 2, 0);

        grid.add(lblInfo, 0, 1);
        grid.add(txtInfo, 1, 1, 2, 1);

        grid.add(new Label("Upload Progress:"), 0, 2);
        grid.add(progressBar, 1, 2, 2, 1);

        grid.add(lblError, 0, 3, 3, 1);

        grid.add(buttonBox, 0, 4, 3, 1);

        // ---- Actions ----
        btnChoose.setOnAction(e -> {
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                selectedFile = file;
                lblFileName.setText(file.getName());
                btnUpload.setDisable(false);
                lblError.setText("");
            }
        });

        btnUpload.setOnAction(e -> {
            if (selectedFile == null) {
                lblError.setText("Select a file first.");
                return;
            }

            Task<Void> task = createUploadTask(selectedFile, txtInfo.getText());

            progressBar.progressProperty().bind(task.progressProperty());

            btnUpload.setDisable(true);
            btnChoose.setDisable(true);
            btnCancel.setDisable(false);

            task.setOnSucceeded(ev -> stage.close());

            task.setOnFailed(ev -> {
                lblError.setText("Upload failed: " + task.getException().getMessage());
                btnUpload.setDisable(false);
                btnChoose.setDisable(false);
            });

            new Thread(task).start();
        });

        btnCancel.setOnAction(e -> {
            try {
                client.close();
            } catch (Exception ignored) {}

            stage.close();
        });

        // ---- SHOW DIALOG ----
        stage.setScene(new Scene(grid, 400, 220));
        stage.showAndWait();
    }
}