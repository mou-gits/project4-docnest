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

public class DownloadDialogFX {

    private final ClientNetwork client;
    private final String filename;

    public DownloadDialogFX(ClientNetwork client, String filename) {
        this.client = client;
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
