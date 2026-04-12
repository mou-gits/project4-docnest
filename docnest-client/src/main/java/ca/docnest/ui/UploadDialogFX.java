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

public class UploadDialogFX {

    private final ClientNetwork client;

    public UploadDialogFX(ClientNetwork client) {
        this.client = client;
    }

    public void showAndWait() {
        Stage stage = new Stage();
        stage.setTitle("Upload File");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label lblFile = new Label("Choose File:");
        Label lblFileName = new Label("(none)");
        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        Button btnChoose = new Button("Browse...");
        Button btnCancel = new Button("Cancel");
        btnCancel.setDisable(true);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(250);

        HBox buttonBox = new HBox(10, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        grid.add(lblFile, 0, 0);
        grid.add(lblFileName, 1, 0);
        grid.add(btnChoose, 2, 0);
        grid.add(new Label("Upload Progress:"), 0, 1);
        grid.add(progressBar, 1, 1, 2, 1);
        grid.add(lblError, 0, 2, 3, 1);
        grid.add(buttonBox, 0, 3, 3, 1);

        btnChoose.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            File file = chooser.showOpenDialog(stage);
            if (file == null) {
                return;
            }

            lblFileName.setText(file.getName());
            btnChoose.setDisable(true);
            btnCancel.setDisable(true);

            Task<Void> task = createUploadTask(file);
            progressBar.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(ev -> stage.close());
            task.setOnFailed(ev -> {
                lblError.setText("Upload failed: " + task.getException().getMessage());
                btnCancel.setDisable(false);
            });

            btnCancel.setOnAction(ev -> stage.close());

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        stage.setScene(new Scene(grid));
        stage.showAndWait();
    }

    private Task<Void> createUploadTask(File file) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(-1, 1);
                client.upload(file.getName(), java.nio.file.Files.readAllBytes(file.toPath()));
                updateProgress(1, 1);
                return null;
            }
        };
    }
}
