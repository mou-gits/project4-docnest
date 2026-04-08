package ca.docnest.ui;

import ca.docnest.backend.BackendFake;
import ca.docnest.model.User;

import javax.swing.*;
import java.awt.*;

public class DownloadDialog extends JDialog {

    private final BackendFake backend;
    private final User user;
    private final String filename;

    private JLabel lblFileName;
    private JProgressBar progressBar;
    private JLabel lblError;
    private JButton btnCancel;

    private boolean downloadSuccess = false;

    public DownloadDialog(JFrame parent, BackendFake backend, User user, String filename) {
        super(parent, "Download File", true);
        this.backend = backend;
        this.user = user;
        this.filename = filename;

        setSize(400, 200);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        initUI();
        startDownload();
    }

    private void initUI() {
        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));

        form.add(new JLabel("File Name:"));
        lblFileName = new JLabel(filename);
        form.add(lblFileName);

        form.add(new JLabel("Status:"));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        form.add(progressBar);

        add(form, BorderLayout.CENTER);

        lblError = new JLabel("");
        lblError.setForeground(Color.RED);
        add(lblError, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnCancel = new JButton("Cancel");
        bottom.add(btnCancel);
        add(bottom, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dispose());
    }

    private void startDownload() {
        btnCancel.setEnabled(false);

        SwingWorker<Boolean, Integer> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Fake progress
                for (int i = 0; i <= 100; i += 5) {
                    Thread.sleep(50);
                    publish(i);
                }

                // Actual backend download
                byte[] data = backend.downloadFile(user.getUserId(), filename);
                return data != null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int value = chunks.get(chunks.size() - 1);
                progressBar.setValue(value);
            }

            @Override
            protected void done() {
                try {
                    downloadSuccess = get();
                    if (downloadSuccess) {
                        dispose();
                    } else {
                        lblError.setText("Download failed.");
                        btnCancel.setEnabled(true);
                    }
                } catch (Exception e) {
                    lblError.setText("Unexpected error.");
                    btnCancel.setEnabled(true);
                }
            }
        };

        worker.execute();
    }


    public boolean wasDownloadSuccessful() {
        return downloadSuccess;
    }
}
