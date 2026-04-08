package ca.docnest.ui;

import ca.docnest.backend.BackendFake;
import ca.docnest.model.User;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class UploadDialog extends JDialog {

    private final BackendFake backend;
    private final User user;

    private JLabel lblFileName;
    private JButton btnChooseFile;
    private JTextField txtInfo;
    private JProgressBar progressBar;
    private JLabel lblError;

    private JButton btnUpload;
    private JButton btnCancel;

    private File selectedFile = null;
    private boolean uploadSuccess = false;

    public UploadDialog(JFrame parent, BackendFake backend, User user) {
        super(parent, "Upload File", true);
        this.backend = backend;
        this.user = user;

        setSize(450, 250);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        initUI();
    }

    public boolean wasUploadSuccessful() {
        return uploadSuccess;
    }

    private void initUI() {
        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));

        // File chooser row
        form.add(new JLabel("File Name:"));
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblFileName = new JLabel("<none>");
        btnChooseFile = new JButton("Choose File");
        filePanel.add(lblFileName);
        filePanel.add(btnChooseFile);
        form.add(filePanel);

        // Additional info row
        form.add(new JLabel("Additional Info:"));
        txtInfo = new JTextField();
        form.add(txtInfo);

        // Progress bar row
        form.add(new JLabel("Upload Progress:"));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        form.add(progressBar);

        add(form, BorderLayout.CENTER);

        // Error label
        lblError = new JLabel("");
        lblError.setForeground(Color.RED);
        add(lblError, BorderLayout.NORTH);

        // Buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnUpload = new JButton("Upload File");
        btnCancel = new JButton("Cancel");
        bottom.add(btnCancel);
        bottom.add(btnUpload);
        add(bottom, BorderLayout.SOUTH);

        // Wire actions
        btnChooseFile.addActionListener(e -> chooseFile());
        btnUpload.addActionListener(e -> startUpload());
        btnCancel.addActionListener(e -> dispose());
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            lblFileName.setText(selectedFile.getName());
        }
    }

    private void startUpload() {
        lblError.setText("");

        if (selectedFile == null) {
            lblError.setText("Please choose a file first.");
            return;
        }

        btnUpload.setEnabled(false);
        btnChooseFile.setEnabled(false);
        btnCancel.setEnabled(false);

        // Simulate progress using SwingWorker
        SwingWorker<Boolean, Integer> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Fake progress
                for (int i = 0; i <= 100; i += 5) {
                    Thread.sleep(50);
                    publish(i);
                }

                // Actual backend upload
                return backend.uploadFile(user.getUserId(), selectedFile, txtInfo.getText());
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int value = chunks.get(chunks.size() - 1);
                progressBar.setValue(value);
            }

            @Override
            protected void done() {
                try {
                    uploadSuccess = get();
                    if (uploadSuccess) {
                        dispose();
                    } else {
                        lblError.setText("Upload failed.");
                        btnUpload.setEnabled(true);
                        btnChooseFile.setEnabled(true);
                        btnCancel.setEnabled(true);
                    }
                } catch (Exception e) {
                    lblError.setText("Unexpected error.");
                }
            }
        };

        worker.execute();
    }


}
