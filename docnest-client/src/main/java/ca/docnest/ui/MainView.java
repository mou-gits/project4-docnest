package ca.docnest.ui;

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

    private TextField txtHost;
    private TextField txtPort;

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
        txtHost = new TextField("localhost");
        txtPort = new TextField("9090");

        btnLogin = new Button("Login");
        btnLogout = new Button("Logout");

        lblStatus = new Label("Logged in as:");

        HBox connectionBar = new HBox(10,
                new Label("Host:"), txtHost,
                new Label("Port:"), txtPort,
                btnLogin,
                btnLogout
        );
        connectionBar.setAlignment(Pos.CENTER_LEFT);

        VBox topBox = new VBox(10, lblStatus, connectionBar);
        root.setTop(topBox);

        // ---------- File List ----------
        fileList = new ListView<>();

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
            client.connect(
                    txtHost.getText(),
                    Integer.parseInt(txtPort.getText())
            );

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

            txtHost.setDisable(true);
            txtPort.setDisable(true);
            btnLogin.setDisable(true);

            btnLogout.setDisable(false);
            btnUpload.setDisable(false);

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

        txtHost.setDisable(false);
        txtPort.setDisable(false);
        btnLogin.setDisable(false);

        btnLogout.setDisable(true);
        btnUpload.setDisable(true);
        btnDownload.setDisable(true);
        btnDelete.setDisable(true);
        btnInfo.setDisable(true);

        fileList.getItems().clear();
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

                        // ===== CONFIGURABLE WIDTHS =====
                        int TOTAL_WIDTH = 90;

                        int NAME_WIDTH = 20;
                        int SIZE_WIDTH = 15;
                        int DATE_WIDTH = 10;

                        int SPACES_BETWEEN = 3; // 3 spaces between columns
                        int INFO_WIDTH = TOTAL_WIDTH - (NAME_WIDTH + SIZE_WIDTH + DATE_WIDTH + SPACES_BETWEEN);

                        // ===== NAME (handle filename + extension) =====
                        String fullName = f.getFilename();

                        String namePart = fullName;
                        String extPart = "";

                        int dotIndex = fullName.lastIndexOf('.');
                        if (dotIndex > 0 && dotIndex < fullName.length() - 1) {
                            namePart = fullName.substring(0, dotIndex);
                            extPart = fullName.substring(dotIndex); // includes "."
                        }

                        String displayName = namePart + extPart;

                        if (displayName.length() > NAME_WIDTH) {
                            // shrink namePart only, preserve extension
                            int keepStart = 10;
                            int keepEnd = 5;

                            if (namePart.length() > (keepStart + keepEnd)) {
                                namePart = namePart.substring(0, keepStart)
                                        + "..."
                                        + namePart.substring(namePart.length() - keepEnd);
                            }

                            displayName = namePart + extPart;

                            // final safety trim if still too long
                            if (displayName.length() > NAME_WIDTH) {
                                displayName = displayName.substring(0, NAME_WIDTH);
                            }
                        }

                        displayName = String.format("%-" + NAME_WIDTH + "s", displayName);

                        // ===== SIZE =====
                        String sizeMB = String.format("%.2f MB", f.getSize() / (1024.0 * 1024.0));
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

                            if (info.length() > INFO_WIDTH) {
                                info = info.substring(0, INFO_WIDTH);
                            }
                        }

                        info = String.format("%-" + INFO_WIDTH + "s", info);

                        // ===== FINAL ROW =====
                        return displayName + " " + sizeMB + " " + date + " " + info;

                    })
                    .toList();

            fileList.setItems(FXCollections.observableArrayList(names));
        });

        task.setOnFailed(e -> showError("Failed to load files: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    // ---------- HELPERS ----------
    private FileMetadata getSelectedFile() {
        int index = fileList.getSelectionModel().getSelectedIndex();
        if (index < 0 || currentFiles == null) return null;
        return currentFiles.get(index);
    }

    private void handleUpload() {
        if (client == null) return;

        new UploadDialogFX(client).showAndWait();
        refreshFileList();
    }

    private void handleDownload() {
        var file = getSelectedFile();
        if (file == null) {
            showError("Select a file first.");
            return;
        }

        new DownloadDialogFX(client, file.getFilename()).showAndWait();
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

        task.setOnFailed(e -> showError("Delete failed: " + task.getException().getMessage()));

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