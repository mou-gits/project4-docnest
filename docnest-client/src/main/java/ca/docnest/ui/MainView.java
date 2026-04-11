package ca.docnest.ui;

import ca.docnest.backend.BackendFake;
import ca.docnest.model.FileMetadata;
import ca.docnest.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class MainView {

    private final BackendFake backend = new BackendFake();

    private User currentUser = null;

    private BorderPane root;

    private Label lblLoggedInAs;
    private Button btnLogin;

    private TableView<FileMetadata> table;
    private ObservableList<FileMetadata> tableData;

    private Button btnUpload;
    private Button btnDownload;
    private Button btnInfo;
    private Button btnDelete;
    private Button btnRefresh;
    private Button btnLogout;

    public MainView() {
        buildUI();
        setLoggedOutState();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildUI() {
        root = new BorderPane();

        // HEADER
        HBox header = new HBox();
        header.setPadding(new Insets(10));
        header.setSpacing(10);
        header.setAlignment(Pos.CENTER_LEFT);

        lblLoggedInAs = new Label("Logged in as: <none>");
        btnLogin = new Button("Login");

        btnLogin.setOnAction(e -> openLoginDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(lblLoggedInAs, spacer, btnLogin);
        root.setTop(header);

        // TABLE
        tableData = FXCollections.observableArrayList();
        table = new TableView<>(tableData);

        TableColumn<FileMetadata, String> colName = new TableColumn<>("File Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("filename"));
        colName.setPrefWidth(250);

        TableColumn<FileMetadata, Long> colSize = new TableColumn<>("Size");
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colSize.setPrefWidth(100);

        TableColumn<FileMetadata, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        colDate.setPrefWidth(120);

        TableColumn<FileMetadata, String> colInfo = new TableColumn<>("Info");
        colInfo.setCellValueFactory(new PropertyValueFactory<>("info"));
        colInfo.setPrefWidth(250);

        table.getColumns().addAll(colName, colSize, colDate, colInfo);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean selected = newSel != null && currentUser != null;
            btnDownload.setDisable(!selected);
            btnInfo.setDisable(!selected);
            btnDelete.setDisable(!selected);
        });

        root.setCenter(table);

        // BUTTON BAR
        HBox bottom = new HBox();
        bottom.setPadding(new Insets(10));
        bottom.setSpacing(10);
        bottom.setAlignment(Pos.CENTER);

        btnUpload = new Button("Upload");
        btnDownload = new Button("Download");
        btnInfo = new Button("Info");
        btnDelete = new Button("Delete");
        btnRefresh = new Button("Refresh");
        btnLogout = new Button("Logout");

        btnUpload.setOnAction(e -> openUploadDialog());
        btnDownload.setOnAction(e -> openDownloadDialog());
        btnInfo.setOnAction(e -> showFileInfo());
        btnDelete.setOnAction(e -> deleteSelectedFile());
        btnRefresh.setOnAction(e -> loadFileList());
        btnLogout.setOnAction(e -> logout());

        bottom.getChildren().addAll(
                btnUpload, btnDownload, btnInfo, btnDelete, btnRefresh, btnLogout
        );

        root.setBottom(bottom);
    }

    private void setLoggedOutState() {
        currentUser = null;
        lblLoggedInAs.setText("Logged in as: <none>");

        table.setDisable(true);
        btnUpload.setDisable(true);
        btnDownload.setDisable(true);
        btnInfo.setDisable(true);
        btnDelete.setDisable(true);
        btnRefresh.setDisable(true);
        btnLogout.setDisable(true);
    }

    private void setLoggedInState(User user) {
        currentUser = user;
        lblLoggedInAs.setText("Logged in as: " + user.getName());

        table.setDisable(false);
        btnUpload.setDisable(false);
        btnRefresh.setDisable(false);
        btnLogout.setDisable(false);

        btnDownload.setDisable(true);
        btnInfo.setDisable(true);
        btnDelete.setDisable(true);

        loadFileList();
    }

    private void loadFileList() {
        tableData.clear();

        if (currentUser == null) return;

        List<FileMetadata> files = backend.listFiles(currentUser.getUserId());
        tableData.addAll(files);
    }

    private void openLoginDialog() {
        LoginDialogFX dialog = new LoginDialogFX(backend);
        User user = dialog.showAndWait();

        if (user != null) {
            setLoggedInState(user);
        }
    }

    private void openUploadDialog() {
        UploadDialogFX dialog = new UploadDialogFX(backend, currentUser);
        boolean success = dialog.showAndWait();

        if (success) {
            loadFileList();
        }
    }

    private void openDownloadDialog() {
        FileMetadata meta = table.getSelectionModel().getSelectedItem();
        if (meta == null) return;

        DownloadDialogFX dialog = new DownloadDialogFX(backend, currentUser, meta.getFilename());
        dialog.showAndWait();
    }

    private void showFileInfo() {
        FileMetadata meta = table.getSelectionModel().getSelectedItem();
        FileInfoDialogFX.show(meta);
    }

    private void deleteSelectedFile() {
        FileMetadata meta = table.getSelectionModel().getSelectedItem();
        if (meta == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete \"" + meta.getFilename() + "\"?",
                ButtonType.YES, ButtonType.NO);

        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait();

        if (confirm.getResult() != ButtonType.YES) return;

        boolean success = backend.deleteFile(currentUser.getUserId(), meta.getFilename());

        if (!success) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to delete file.", ButtonType.OK);
            err.showAndWait();
            return;
        }

        loadFileList();
    }

    private void logout() {
        setLoggedOutState();
        tableData.clear();
    }
}
