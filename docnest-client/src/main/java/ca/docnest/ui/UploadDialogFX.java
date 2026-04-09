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

public class UploadDialogFX {

    private final BackendFake backend;
    private final User user;

    private File selectedFile = null;
    private boolean uploadSuccess = false;

    public UploadDialogFX(BackendFake backend, User user) {
        this.backend = backend;
        this.user = user;
    }

    public boolean showAndWait() {
        Stage stage = new Stage();
        stage.setTitle("Upload File");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label lblFile = new Label("File Name:");
        Label lblFileName = new Label("<none>");
        Button btnChoose = new Button("Choose File");

        HBox fileBox = new HBox(10, lblFileName, btnChoose);
        fileBox.setAlignment(Pos.CENTER_LEFT);

        Label lblInfo = new Label("Additional Info:");
        TextField txtInfo = new TextField();

        Label lblProgress = new Label("Upload Progress:");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        Button btnUpload = new Button("Upload File");
        Button btnCancel = new Button("Cancel");

        HBox buttonBox = new HBox(10, btnCancel, btnUpload);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        grid.add(lblFile, 0, 0);
        grid.add(fileBox, 1, 0);

        grid.add(lblInfo, 0, 1);
        grid.add(txtInfo, 1, 1);

        grid.add(lblProgress, 0, 2);
        grid.add(progressBar, 1, 2);

        grid.add(lblError, 0, 3, 2, 1);

        grid.add(buttonBox, 0, 4, 2, 1);

        // Choose file action
        btnChoose.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                selectedFile = file;
                lblFileName.setText(file.getName());
            }
        });

        // Upload action
        btnUpload.setOnAction(e -> {
            lblError.setText("");

            if (selectedFile == null) {
                lblError.setText("Please choose a file first.");
                return;
            }

            btnUpload.setDisable(true);
            btnChoose.setDisable(true);
            btnCancel.setDisable(true);

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    // Fake progress
                    for (int i = 0; i <= 100; i += 5) {
                        Thread.sleep(50);
                        updateProgress(i, 100);
                    }

                    // Actual backend upload
                    return backend.uploadFile(user.getUserId(), selectedFile, txtInfo.getText());
                }
            };

            progressBar.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(ev -> {
                uploadSuccess = task.getValue();
                if (uploadSuccess) {
                    stage.close();
                } else {
                    lblError.setText("Upload failed.");
                    btnUpload.setDisable(false);
                    btnChoose.setDisable(false);
                    btnCancel.setDisable(false);
                }
            });

            task.setOnFailed(ev -> {
                lblError.setText("Unexpected error.");
                btnUpload.setDisable(false);
                btnChoose.setDisable(false);
                btnCancel.setDisable(false);
            });

            new Thread(task).start();
        });

        btnCancel.setOnAction(e -> stage.close());

        Scene scene = new Scene(grid, 450, 250);
        stage.setScene(scene);
        stage.showAndWait();

        return uploadSuccess;
    }
}
