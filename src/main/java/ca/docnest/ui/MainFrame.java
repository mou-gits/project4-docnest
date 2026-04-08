package ca.docnest.ui;

import ca.docnest.backend.BackendFake;
import ca.docnest.model.FileMetadata;
import ca.docnest.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {

    private final BackendFake backend;

    private User currentUser;

    private JLabel lblLoggedInAs;
    private JButton btnLogin;

    private JTable fileTable;
    private DefaultTableModel tableModel;

    private JButton btnUpload;
    private JButton btnDownload;
    private JButton btnInfo;
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnLogout;

    public MainFrame() {
        super("DocNest Client");

        backend = new BackendFake();

        initUI();
        setLoggedOutState();

        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildFileTablePanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        lblLoggedInAs = new JLabel("Logged in as: <none>");
        btnLogin = new JButton("Login");

        btnLogin.addActionListener(e -> openLoginDialog());

        panel.add(lblLoggedInAs, BorderLayout.WEST);
        panel.add(btnLogin, BorderLayout.EAST);

        return panel;
    }

    private JPanel buildFileTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        tableModel = new DefaultTableModel(
                new Object[]{"File Name", "Size", "Date", "Info"}, 0
        );

        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        fileTable.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = fileTable.getSelectedRow() != -1;
            btnDownload.setEnabled(selected && currentUser != null);
            btnInfo.setEnabled(selected && currentUser != null);
            btnDelete.setEnabled(selected && currentUser != null);
        });

        panel.add(new JScrollPane(fileTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        btnUpload = new JButton("Upload");
        btnDownload = new JButton("Download");
        btnInfo = new JButton("Info");
        btnDelete = new JButton("Delete");
        btnRefresh = new JButton("Refresh");
        btnLogout = new JButton("Logout");

        btnUpload.addActionListener(e -> openUploadDialog());
        btnDownload.addActionListener(e -> openDownloadDialog());
        btnInfo.addActionListener(e -> showFileInfo());
        btnDelete.addActionListener(e -> deleteSelectedFile());
        btnRefresh.addActionListener(e -> loadFileList());
        btnLogout.addActionListener(e -> logout());

        panel.add(btnUpload);
        panel.add(btnDownload);
        panel.add(btnInfo);
        panel.add(btnDelete);
        panel.add(btnRefresh);
        panel.add(btnLogout);


        return panel;
    }

    private void setLoggedOutState() {
        currentUser = null;
        lblLoggedInAs.setText("Logged in as: <none>");

        fileTable.setEnabled(false);
        btnUpload.setEnabled(false);
        btnDownload.setEnabled(false);
        btnInfo.setEnabled(false);
        btnDelete.setEnabled(false);
        btnRefresh.setEnabled(false);
        btnLogout.setEnabled(false);
    }

    private void setLoggedInState(User user) {
        currentUser = user;
        lblLoggedInAs.setText("Logged in as: " + user.getName());

        fileTable.setEnabled(true);
        btnUpload.setEnabled(true);
        btnRefresh.setEnabled(true);
        btnLogout.setEnabled(true);

        // Download/Info depend on selection
        btnDownload.setEnabled(false);
        btnInfo.setEnabled(false);
        btnDelete.setEnabled(false);

        loadFileList();
    }

    private void loadFileList() {
        tableModel.setRowCount(0);

        if (currentUser == null) return;

        List<FileMetadata> files = backend.listFiles(currentUser.getUserId());

        for (FileMetadata m : files) {
            tableModel.addRow(new Object[]{
                    m.getFilename(),
                    m.getSize(),
                    m.getUploadDate(),
                    m.getInfo()
            });
        }
    }

    private void openLoginDialog() {
        LoginDialog dialog = new LoginDialog(this, backend);
        dialog.setVisible(true);

        User user = dialog.getAuthenticatedUser();
        if (user != null) {
            setLoggedInState(user);
        }
    }

    private void openUploadDialog() {
        UploadDialog dialog = new UploadDialog(this, backend, currentUser);
        dialog.setVisible(true);

        if (dialog.wasUploadSuccessful()) {
            loadFileList();
        }
    }

    private void openDownloadDialog() {
        int row = fileTable.getSelectedRow();
        if (row == -1) return;

        String filename = (String) tableModel.getValueAt(row, 0);

        DownloadDialog dialog = new DownloadDialog(this, backend, currentUser, filename);
        dialog.setVisible(true);

        // No need to refresh table after download
    }

    private void showFileInfo() {
        int row = fileTable.getSelectedRow();
        if (row == -1) return;

        String filename = (String) tableModel.getValueAt(row, 0);

        // Load metadata list
        var list = backend.listFiles(currentUser.getUserId());

        FileMetadata meta = list.stream()
                .filter(m -> m.getFilename().equals(filename))
                .findFirst()
                .orElse(null);

        if (meta == null) return;

        String message = """
            File Name: %s
            Owner: %s
            Size: %d bytes
            Type: %s
            Upload Date: %s
            Additional Info: %s
            """.formatted(
                meta.getFilename(),
                meta.getUploadedBy(),
                meta.getSize(),
                meta.getType(),
                meta.getUploadDate(),
                meta.getInfo()
        );

        JOptionPane.showMessageDialog(this, message, "File Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedFile() {
        int row = fileTable.getSelectedRow();
        if (row == -1) return;

        String filename = (String) tableModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete \"" + filename + "\"?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        boolean success = backend.deleteFile(currentUser.getUserId(), filename);

        if (!success) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to delete file.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        loadFileList();
    }


    private void logout() {
        setLoggedOutState();
        tableModel.setRowCount(0);
    }



}
