package ca.docnest.ui;

import ca.docnest.client.network.ClientNetwork;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class UploadDialogFX {

    private final ClientNetwork client;

    private File selectedFile;

    public UploadDialogFX(ClientNetwork client) {
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
        FileChooser chooser = new FileChooser();

        Stage stage = new Stage();
        stage.setTitle("Upload File");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label lblInfo = new Label("Additional Info:");
        TextField txtInfo = new TextField();
        txtInfo.setPrefWidth(250);

        Label lblFile = new Label("Choose File:");
        Label lblFileName = new Label("(none)");
        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        Button btnChoose = new Button("Browse...");
        Button btnCancel = new Button("Cancel");
        Button btnUpload = new Button("Upload");
        btnUpload.setDisable(true);
        btnCancel.setDisable(true);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(250);

        HBox buttonBox = new HBox(10, btnCancel, btnUpload);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

// ---- ROW 0 (File chooser)
        grid.add(lblFile, 0, 0);
        grid.add(lblFileName, 1, 0);
        grid.add(btnChoose, 2, 0);

// ---- ROW 1 (Metadata)
        grid.add(lblInfo, 0, 1);
        grid.add(txtInfo, 1, 1, 2, 1);

// ---- ROW 2 (Progress)
        grid.add(new Label("Upload Progress:"), 0, 2);
        grid.add(progressBar, 1, 2, 2, 1);

// ---- ROW 3 (Error)
        grid.add(lblError, 0, 3, 3, 1);

// ---- ROW 4 (Buttons)
        grid.add(buttonBox, 0, 4, 3, 1);



        btnChoose.setOnAction(e -> {
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                selectedFile = file;
                lblFileName.setText(file.getName());
                btnUpload.setDisable(false);
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
            btnCancel.setDisable(false);

            task.setOnSucceeded(ev -> stage.close());
            task.setOnFailed(ev -> {
                lblError.setText("Upload failed: " + task.getException().getMessage());
                btnUpload.setDisable(false);
            });

            new Thread(task).start();
        });
    }
}
