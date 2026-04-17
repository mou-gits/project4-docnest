package ca.docnest.ui;

import ca.docnest.client.config.Config;
import ca.docnest.client.network.ClientNetwork;
import ca.docnest.shared.model.FileMetadata;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class MainView {

    private List<FileMetadata> currentFiles;

    private ClientNetwork client;
    private String currentUser;

    private Button btnLogin;
    private Button btnLogout;
    private Button btnUpload;
    private Button btnDownload;
    private Button btnDelete;
    private Button btnInfo;
    private Button btnRefresh;

    private Label lblStatus;

    private final Stage stage;
    private ListView<String> fileList;

    public MainView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // ---------- Top UI ----------
//        txtHost = new TextField("localhost");
//        txtPort = new TextField("9090");

        btnLogin = new Button("Login");
        btnLogout = new Button("Logout");

        lblStatus = new Label("Logged in as:");

        HBox leftBox = new HBox(lblStatus);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        HBox rightBox = new HBox(10, btnLogin, btnLogout);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topBar = new BorderPane();
        topBar.setLeft(leftBox);
        topBar.setRight(rightBox);
        topBar.setPadding(new Insets(10)); // optional, looks nicer

        root.setTop(topBar);

        // ---------- File List ----------
        fileList = new ListView<>();
        fileList.setStyle("-fx-font-family: 'monospace';");

        // ---------- Buttons ----------
        btnUpload = new Button("Upload");
        btnDownload = new Button("Download");
        btnDelete = new Button("Delete");
        btnRefresh = new Button("Refresh");
        btnInfo = new Button("Info");

        // Initial state
        btnLogout.setDisable(true);
        btnUpload.setDisable(true);
        btnDownload.setDisable(true);
        btnDelete.setDisable(true);
        btnInfo.setDisable(true);
        btnRefresh.setDisable(true);

        // Selection listener
        fileList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean selected = newVal != null && client != null;

            btnDownload.setDisable(!selected);
            btnDelete.setDisable(!selected);
            btnInfo.setDisable(!selected);
        });

        // Button actions
        btnLogin.setOnAction(e -> handleLogin());
        btnLogout.setOnAction(e -> handleLogout());

        btnUpload.setOnAction(e -> handleUpload());
        btnDownload.setOnAction(e -> handleDownload());
        btnDelete.setOnAction(e -> handleDelete());
        btnRefresh.setOnAction(e -> refreshFileList());
        btnInfo.setOnAction(e -> handleInfo());

        HBox buttonBar = new HBox(10, btnUpload, btnDownload, btnDelete, btnInfo, btnRefresh);
        buttonBar.setAlignment(Pos.CENTER);

        VBox centerBox = new VBox(10, new Label("Your Files:"), fileList, buttonBar);
        centerBox.setPadding(new Insets(10));
        root.setCenter(centerBox);

        stage.setScene(new Scene(root, 800, 500));
        stage.setTitle("DocNest - Dashboard");
        stage.show();
    }

    // ---------- LOGIN ----------
    private void handleLogin() {
        try {
            // 1. Create client + connect
            client = new ClientNetwork();
            client.connect(Config.HOST, Config.PORT);

            // 2. Show login dialog
            LoginDialogFX dialog = new LoginDialogFX(stage);
            dialog.showAndWait();

            String username = dialog.getUsername();
            String password = dialog.getPassword();

            if (username == null) {
                client.close();
                client = null;
                return;
            }

            // 3. Perform login
            boolean success = client.login(username, password);

            if (!success) {
                lblStatus.setText("Login failed: invalid credentials");
                lblStatus.setStyle("-fx-text-fill: red;");
                client.close();
                client = null;
                return;
            }

            // 4. SUCCESS UI
            currentUser = username;

            lblStatus.setText("Logged in as: " + username);
            lblStatus.setStyle("-fx-text-fill: green;");

            btnLogin.setDisable(true);

            btnLogout.setDisable(false);
            btnUpload.setDisable(false);
            btnRefresh.setDisable(false);
            refreshFileList();

        } catch (Exception ex) {
            lblStatus.setText("Login failed: " + ex.getMessage());
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }
    // ---------- LOGOUT ----------
    private void handleLogout() {
        try {
            if (client != null) {
                client.logout();
            }
        } catch (Exception ignored) {}

        client = null;
        currentUser = null;

        lblStatus.setText("Logged in as:");

        btnLogin.setDisable(false);

        btnLogout.setDisable(true);
        btnUpload.setDisable(true);
        btnDownload.setDisable(true);
        btnDelete.setDisable(true);
        btnInfo.setDisable(true);
        btnRefresh.setDisable(true);
        fileList.getItems().clear();
    }

    private void resetToLoggedOutState(String message) {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception ignored) {}

        new MainView(stage).show();

        // Optional: show message
        if (message != null) {
            System.out.println(message);
        }
    }

    // ---------- FILE LIST ----------
    private void refreshFileList() {
        if (client == null) return;

        Task<List<FileMetadata>> task = new Task<>() {
            @Override
            protected List<FileMetadata> call() throws Exception {
                return client.listFiles();
            }
        };

        task.setOnSucceeded(e -> {
            currentFiles = task.getValue();

            var names = currentFiles.stream()
                    .map(f -> {

                        // ===== CONFIG =====
                        int TOTAL_WIDTH = 120;

                        int NAME_WIDTH = 40;
                        int SIZE_WIDTH = 10;
                        int DATE_WIDTH = 20;

                        int SPACES_BETWEEN = 3;
                        String GAP = " ".repeat(SPACES_BETWEEN);

                        int INFO_WIDTH = TOTAL_WIDTH - (NAME_WIDTH + SIZE_WIDTH + DATE_WIDTH + SPACES_BETWEEN * 3);

                        // ===== NAME =====
                        String fullName = f.getFilename();

                        String namePart = fullName;
                        String extPart = "";

                        int dotIndex = fullName.lastIndexOf('.');
                        if (dotIndex > 0 && dotIndex < fullName.length() - 1) {
                            namePart = fullName.substring(0, dotIndex);
                            extPart = fullName.substring(dotIndex);
                        }

                        String displayName = namePart + extPart;

                        if (displayName.length() > NAME_WIDTH) {
                            int keepStart = 10;
                            int keepEnd = 5;

                            if (namePart.length() > (keepStart + keepEnd)) {
                                namePart = namePart.substring(0, keepStart)
                                        + "..."
                                        + namePart.substring(namePart.length() - keepEnd);
                            }

                            displayName = namePart + extPart;
                        }

                        // enforce exact width
                        if (displayName.length() > NAME_WIDTH) {
                            displayName = displayName.substring(0, NAME_WIDTH);
                        }
                        displayName = String.format("%-" + NAME_WIDTH + "s", displayName);

                        // ===== SIZE =====
                        String sizeMB = String.format("%.2f MB", f.getSize() / (1024.0 * 1024.0));
                        if (sizeMB.length() > SIZE_WIDTH) {
                            sizeMB = sizeMB.substring(0, SIZE_WIDTH);
                        }
                        sizeMB = String.format("%-" + SIZE_WIDTH + "s", sizeMB);

                        // ===== DATE =====
                        String date = f.getUploadDate();
                        if (date.length() > DATE_WIDTH) {
                            date = date.substring(0, DATE_WIDTH);
                        }
                        date = String.format("%-" + DATE_WIDTH + "s", date);

                        // ===== INFO =====
                        String info = (f.getAdditionalInfo() == null || f.getAdditionalInfo().isBlank())
                                ? "-"
                                : f.getAdditionalInfo();

                        if (info.length() > INFO_WIDTH) {
                            int keepStart = 10;
                            int keepEnd = 5;

                            if (info.length() > (keepStart + keepEnd)) {
                                info = info.substring(0, keepStart)
                                        + "..."
                                        + info.substring(info.length() - keepEnd);
                            }
                        }

                        if (info.length() > INFO_WIDTH) {
                            info = info.substring(0, INFO_WIDTH);
                        }
                        info = String.format("%-" + INFO_WIDTH + "s", info);

                        // ===== FINAL ROW =====
                        return displayName + GAP + sizeMB + GAP + date + GAP + info;

                    })
                    .toList();

            fileList.setItems(FXCollections.observableArrayList(names));
        });

        task.setOnFailed(e -> {
            resetToLoggedOutState("Connection lost. Please log in again.");
        });

        new Thread(task).start();
    }

    // ---------- HELPERS ----------
    private FileMetadata getSelectedFile() {
        int index = fileList.getSelectionModel().getSelectedIndex();
        if (index < 0 || currentFiles == null) return null;
        return currentFiles.get(index);
    }

    private void showConnectionLostDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Lost");
        alert.setHeaderText("Session Ended");
        alert.setContentText(message != null
                ? message
                : "Connection lost. Please log in again.");

        alert.showAndWait();
    }

    private void handleUpload() {
        if (client == null) return;

        try {
            new UploadDialogFX(stage, client).showAndWait();
            refreshFileList();
        } catch (Exception ex) {
            resetToLoggedOutState("Connection lost. Please log in again.");
        }
    }
    private void handleDownload() {
        var file = getSelectedFile();
        if (file == null) {
            showError("Select a file first.");
            return;
        }

        try {
            new DownloadDialogFX(client, file.getFilename()).showAndWait();
        } catch (Exception ex) {
            resetToLoggedOutState("Connection lost. Please log in again.");
        }
    }

    private void handleDelete() {
        var file = getSelectedFile();
        if (file == null) {
            showError("Select a file first.");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                client.delete(file.getFilename());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showInfo("File deleted.");
            refreshFileList();
        });

        task.setOnFailed(e -> {

            Throwable ex = task.getException();

            // If it's a controlled error (server responded with ERROR)
            if (ex instanceof java.io.IOException &&
                    ex.getMessage() != null &&
                    ex.getMessage().startsWith("Delete failed")) {

                showError(ex.getMessage());   // just show error
                return;
            }

            // Otherwise it's a real connection issue
            showConnectionLostDialog("Connection lost during operation.\nPlease log in again.");
            resetToLoggedOutState("Connection lost. Please log in again.");
        });

        new Thread(task).start();
    }

    private void handleInfo() {
        var file = getSelectedFile();
        if (file == null) {
            showError("Select a file first.");
            return;
        }

        FileInfoDialogFX.show(file);
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}