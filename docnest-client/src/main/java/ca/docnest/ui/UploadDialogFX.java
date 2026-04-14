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

public class UploadDialogFX {

    private final ClientNetwork client;
    private final Stage owner;

    private File selectedFile;

    public UploadDialogFX(Stage owner, ClientNetwork client) {
        this.owner = owner;
        this.client = client;
    }

    private Task<Void> createUploadTask(File file, String info) {
        return new Task<>() {
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