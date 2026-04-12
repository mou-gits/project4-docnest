package ca.docnest.ui;

import ca.docnest.client.network.ClientNetwork;
import ca.docnest.shared.protocol.PacketBuilder;
import ca.docnest.shared.protocol.PacketParser;
import ca.docnest.shared.protocol.PacketType;
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

        Label lblFile = new Label("File Name:");
        Label lblFileName = new Label(filename);

        Label lblProgress = new Label("Download Progress:");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(250);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        Button btnCancel = new Button("Cancel");
        btnCancel.setDisable(true);

        HBox buttonBox = new HBox(10, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        grid.add(lblFile, 0, 0);
        grid.add(lblFileName, 1, 0);

        grid.add(lblProgress, 0, 1);
        grid.add(progressBar, 1, 1);

        grid.add(lblError, 0, 2, 2, 1);
        grid.add(buttonBox, 0, 3, 2, 1);

        Scene scene = new Scene(grid);
        stage.setScene(scene);

        // -----------------------------
        // REAL DOWNLOAD TASK
        // -----------------------------
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {

                // Step 1: Request download metadata
                // (ClientNetwork.download() already handles this)
                // But we need progress, so we reimplement the logic here.

                // Send DOWNLOAD_INIT
                var initPacket = PacketBuilder.buildDownloadInitPacket(filename);
                client.getTransport().send(initPacket);

                // Receive META
                var meta = client.getTransport().receive();
                var metaJson = PacketParser.parseJson(meta);
                int size = metaJson.get("size").asInt();

                byte[] buffer = new byte[size];
                int offset = 0;

                // Step 2: Receive chunks
                while (true) {
                    var p = client.getTransport().receive();

                    if (p.getCommand() == PacketType.DOWNLOAD_COMPLETE) {
                        break;
                    }

                    if (p.getCommand() != PacketType.DOWNLOAD_CHUNK) {
                        throw new Exception("Unexpected packet during download");
                    }

                    byte[] chunk = PacketParser.parseChunk(p);
                    System.arraycopy(chunk, 0, buffer, offset, chunk.length);
                    offset += chunk.length;

                    updateProgress(offset, size);
                }

                return buffer;
            }
        };

        // Bind progress bar
        progressBar.progressProperty().bind(task.progressProperty());

        // On success
        task.setOnSucceeded(ev -> {
            byte[] data = task.getValue();

            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(filename);
            File saveTo = chooser.showSaveDialog(stage);

            if (saveTo != null) {
                try {
                    Files.write(saveTo.toPath(), data);
                } catch (Exception e) {
                    lblError.setText("Failed to save file.");
                }
            }

            stage.close();
        });

        // On failure
        task.setOnFailed(ev -> {
            lblError.setText("Download failed: " + task.getException().getMessage());
            btnCancel.setDisable(false);
        });

        // Cancel button closes dialog
        btnCancel.setOnAction(e -> stage.close());

        // Start background thread
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

        stage.showAndWait();
    }
}
