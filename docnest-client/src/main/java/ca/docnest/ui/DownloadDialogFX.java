package ca.docnest.ui;

import ca.docnest.client.network.ClientNetwork;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

/**
 * @class DownloadDialogFX
 * @brief Provides a JavaFX modal dialog for downloading files from the server.
 *
 * @details
 * The {@code DownloadDialogFX} class creates and displays a user interface
 * dialog that allows a user to download a selected file from the DocNest
 * server using the {@link ClientNetwork} communication layer.
 *
 * The dialog presents:
 * <ul>
 *   <li>The selected file name</li>
 *   <li>A progress bar indicating download activity</li>
 *   <li>Error messages when failures occur</li>
 *   <li>A cancel button for closing the dialog after failure</li>
 * </ul>
 *
 * The actual download operation is executed in a background JavaFX
 * {@link Task} to prevent blocking the user interface thread.
 *
 * After a successful download, the user is prompted with a
 * {@link FileChooser} dialog to choose where the file should be saved locally.
 *
 * This class is intended to be used as a reusable modal component within the
 * DocNest client application.
 */
public class DownloadDialogFX {

    /**
     * @brief Client network service used to communicate with the server.
     *
     * @details
     * This reference is used to send the download request and retrieve file
     * contents from the remote DocNest server.
     */
    private final ClientNetwork client;

    /**
     * @brief Name of the file to be downloaded.
     *
     * @details
     * This value is displayed in the dialog and passed to the server when
     * initiating the download request.
     */
    private final String filename;

    /**
     * @brief Constructs a download dialog for a specific file.
     *
     * @details
     * Initializes the dialog with the network client used for communication
     * and the target filename to download.
     *
     * @param client The active {@link ClientNetwork} instance used to connect
     *               to the server.
     * @param filename The name of the file the user wants to download.
     */
    public DownloadDialogFX(ClientNetwork client, String filename) {
        this.client = client;
        this.filename = filename;
    }

    /**
     * @brief Displays the modal download dialog and waits until it closes.
     *
     * @details
     * This method creates the complete JavaFX dialog window, initializes the
     * layout controls, and starts the background download task.
     *
     * The workflow is as follows:
     * <ol>
     *   <li>Create and configure the modal stage</li>
     *   <li>Display file information and progress controls</li>
     *   <li>Start a background task to download the file</li>
     *   <li>Update the progress bar during download</li>
     *   <li>Prompt the user to choose a save location on success</li>
     *   <li>Write the downloaded bytes to disk</li>
     *   <li>Show error messages if download or save fails</li>
     * </ol>
     *
     * The method blocks further interaction with the parent application window
     * until the dialog is closed because it uses
     * {@code Modality.APPLICATION_MODAL}.
     *
     * @note
     * The progress bar is set to indeterminate mode initially by using
     * {@code updateProgress(-1, 1)} because exact chunk progress is not tracked
     * by the current implementation.
     */
    public void showAndWait() {
        Stage stage = new Stage();
        stage.setTitle("Download File");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(250);

        Button btnCancel = new Button("Cancel");
        btnCancel.setDisable(true);

        HBox buttonBox = new HBox(10, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        grid.add(new Label("File Name:"), 0, 0);
        grid.add(new Label(filename), 1, 0);
        grid.add(new Label("Download Progress:"), 0, 1);
        grid.add(progressBar, 1, 1);
        grid.add(lblError, 0, 2, 2, 1);
        grid.add(buttonBox, 0, 3, 2, 1);

        Task<byte[]> task = new Task<>() {

            /**
             * @brief Executes the file download in a background thread.
             *
             * @details
             * This method is automatically called when the JavaFX task starts.
             * It performs the server download request through the client network
             * object and returns the downloaded byte array.
             *
             * Progress is updated before and after the download operation.
             *
             * @return A byte array containing the downloaded file contents.
             *
             * @throws Exception Thrown if the download operation fails due to
             *                   communication errors or server-side issues.
             */
            @Override
            protected byte[] call() throws Exception {
                updateProgress(-1, 1);
                byte[] data = client.download(filename);
                updateProgress(1, 1);
                return data;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(ev -> {
            byte[] data = task.getValue();
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(filename);
            File saveTo = chooser.showSaveDialog(stage);

            if (saveTo != null) {
                try {
                    java.nio.file.Files.write(saveTo.toPath(), data);
                } catch (Exception e) {
                    lblError.setText("Failed to save file.");
                    return;
                }
            }

            stage.close();
        });

        task.setOnFailed(ev -> {
            lblError.setText("Download failed: " + task.getException().getMessage());
            btnCancel.setDisable(false);
        });

        btnCancel.setOnAction(e -> stage.close());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

        stage.setScene(new Scene(grid));
        stage.showAndWait();
    }
}