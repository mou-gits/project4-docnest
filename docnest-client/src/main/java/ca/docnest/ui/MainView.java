package ca.docnest.ui;

import ca.docnest.client.network.ClientNetwork;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainView {

    private final ClientNetwork client;
    private final Stage stage;

    private ListView<String> fileList;

    public MainView(Stage stage, ClientNetwork client) {
        this.stage = stage;
        this.client = client;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        fileList = new ListView<>();
        refreshFileList();

        Button btnUpload = new Button("Upload");
        Button btnDownload = new Button("Download");
        Button btnDelete = new Button("Delete");
        Button btnRefresh = new Button("Refresh");
        Button btnLogout = new Button("Logout");

        btnUpload.setOnAction(e -> handleUpload());
        btnDownload.setOnAction(e -> handleDownload());
        btnDelete.setOnAction(e -> handleDelete());
        btnRefresh.setOnAction(e -> refreshFileList());
        btnLogout.setOnAction(e -> handleLogout());

        HBox buttonBar = new HBox(10, btnUpload, btnDownload, btnDelete, btnRefresh, btnLogout);
        buttonBar.setAlignment(Pos.CENTER);

        VBox centerBox = new VBox(10, new Label("Your Files:"), fileList, buttonBar);
        centerBox.setPadding(new Insets(10));
        root.setCenter(centerBox);

        stage.setScene(new Scene(root, 600, 400));
        stage.setTitle("DocNest - Dashboard");
        stage.show();
    }

    private void refreshFileList() {
        Task<String[]> task = new Task<>() {
            @Override
            protected String[] call() throws Exception {
                return client.listFiles();
            }
        };

        task.setOnSucceeded(e -> fileList.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> showError("Failed to load file list: " + task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleUpload() {
        new UploadDialogFX(client).showAndWait();
        refreshFileList();
    }

    private void handleDownload() {
        String filename = fileList.getSelectionModel().getSelectedItem();
        if (filename == null) {
            showError("Select a file first.");
            return;
        }

        new DownloadDialogFX(client, filename).showAndWait();
    }

    private void handleDelete() {
        String filename = fileList.getSelectionModel().getSelectedItem();
        if (filename == null) {
            showError("Select a file first.");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                client.delete(filename);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showInfo("File deleted.");
            refreshFileList();
        });
        task.setOnFailed(e -> showError("Delete failed: " + task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleLogout() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                client.logout();
                return null;
            }
        };

        task.setOnSucceeded(e -> stage.close());
        task.setOnFailed(e -> showError("Logout failed: " + task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
