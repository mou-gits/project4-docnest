package ca.docnest.ui;

import ca.docnest.client.network.ClientNetwork;
import ca.docnest.shared.protocol.*;
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

        Button btnChoose = new Button("Browse...");
        Button btnCancel = new Button("Cancel");
        btnCancel.setDisable(true);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(250);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        HBox buttonBox = new HBox(10, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        grid.add(lblFile, 0, 0);
        grid.add(lblFileName, 1, 0);
        grid.add(btnChoose, 2, 0);

        grid.add(new Label("Upload Progress:"), 0, 1);
        grid.add(progressBar, 1, 1, 2, 1);

        grid.add(lblError, 0, 2, 3, 1);
        grid.add(buttonBox, 0, 3, 3, 1);

        Scene scene = new Scene(grid);
        stage.setScene(scene);

        final File[] selectedFile = {null};

        btnChoose.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            File file = chooser.showOpenDialog(stage);

            if (file == null) return;

            selectedFile[0] = file;
            lblFileName.setText(file.getName());

            btnChoose.setDisable(true);
            btnCancel.setDisable(true);

            Task<Void> task = createUploadTask(file, progressBar, lblError, stage);

            progressBar.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(ev -> stage.close());
            task.setOnFailed(ev -> {
                lblError.setText("Upload failed: " + task.getException().getMessage());
                btnCancel.setDisable(false);
            });

            btnCancel.setOnAction(ev -> stage.close());

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });

        stage.showAndWait();
    }

    private Task<Void> createUploadTask(File file, ProgressBar progressBar, Label lblError, Stage stage) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {

                byte[] data = Files.readAllBytes(file.toPath());

                // 1. INIT
                var init = PacketBuilder.buildUploadInitPacket(file.getName(), data.length);
                client.getTransport().send(init);

                var ack = client.getTransport().receive();
                if (ack.getCommand() != PacketType.UPLOAD_ACK)
                    throw new Exception("Upload init failed");

                // 2. CHUNKS
                int offset = 0;
                for (byte[] chunk : PacketBuilder.splitIntoChunks(data)) {

                    var chunkPacket = PacketBuilder.buildChunkPacket(PacketType.UPLOAD_CHUNK, chunk);
                    client.getTransport().send(chunkPacket);

                    var chunkAck = client.getTransport().receive();
                    if (chunkAck.getCommand() != PacketType.CHUNK_ACK)
                        throw new Exception("Chunk upload failed");

                    offset += chunk.length;
                    updateProgress(offset, data.length);
                }

                // 3. COMPLETE
                var complete = PacketBuilder.buildUploadCompletePacket();
                client.getTransport().send(complete);

                var done = client.getTransport().receive();
                if (done.getCommand() != PacketType.UPLOAD_COMPLETE_RESPONSE)
                    throw new Exception("Upload did not complete");

                return null;
            }
        };
    }
}
