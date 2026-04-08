package ca.docnest.ui;

import ca.docnest.backend.BackendFake;
import ca.docnest.model.User;
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
import java.nio.file.Files;

public class DownloadDialogFX {

    private final BackendFake backend;
    private final User user;
    private final String filename;

    public DownloadDialogFX(BackendFake backend, User user, String filename) {
        this.backend = backend;
        this.user = user;
        this.filename = filename;
    }

    public void showAndWait() {
        Stage stage = new Stage();
        stage.setTitle("Download File");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label lblFile = new Label("File Name:");
        Label lblFileName = new Label(filename);

        Label lblProgress = new Label("Download Progress:");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        Button btnCancel = new Button("Cancel");

        HBox buttonBox = new HBox(10, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        grid.add(lblFile, 0, 0);
        grid.add(lblFileName, 1, 0);

        grid.add(lblProgress, 0, 1);
        grid.add(progressBar, 1, 1);

        grid.add(lblError, 0, 2, 2, 1);

        grid.add(buttonBox, 0, 3, 2, 1);

        btnCancel.setOnAction(e -> stage.close());

        // Start download immediately
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // Fake progress
                for (int i = 0; i <= 100; i += 5) {
                    Thread.sleep(50);
                    updateProgress(i, 100);
                }

                // Actual backend download
                byte[] data = backend.downloadFile(user.getUserId(), filename);
                return data != null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(ev -> {
            boolean backendSuccess = task.getValue();

            if (!backendSuccess) {
                lblError.setText("Download failed.");
                btnCancel.setDisable(false);
                return;
            }

            // Retrieve file bytes again
            byte[] data = backend.downloadFile(user.getUserId(), filename);
            if (data == null) {
                lblError.setText("Download failed.");
                btnCancel.setDisable(false);
                return;
            }

            // Ask user where to save
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(filename);
            File saveTo = chooser.showSaveDialog(stage);

            if (saveTo != null) {
                try {
                    Files.write(saveTo.toPath(), data);
                    stage.close();
                } catch (Exception ex) {
                    lblError.setText("Failed to save file.");
                    btnCancel.setDisable(false);
                }
            } else {
                lblError.setText("Download cancelled.");
                btnCancel.setDisable(false);
            }
        });

        task.setOnFailed(ev -> {
            lblError.setText("Unexpected error.");
            btnCancel.setDisable(false);
        });

        new Thread(task).start();

        Scene scene = new Scene(grid, 400, 200);
        stage.setScene(scene);
        stage.showAndWait();
    }
}
