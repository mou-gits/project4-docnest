package ca.docnest.ui;

import ca.docnest.backend.BackendFake;
import ca.docnest.model.User;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;

    private User authenticatedUser = null;

    public LoginDialog(JFrame parent, BackendFake backend) {
        super(parent, "Login", true);

        setLayout(new BorderLayout());
        setSize(350, 200);
        setLocationRelativeTo(parent);

        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));

        form.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        form.add(txtUsername);

        form.add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        form.add(txtPassword);

        lblError = new JLabel("");
        lblError.setForeground(Color.RED);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnLogin = new JButton("Login");
        JButton btnCancel = new JButton("Cancel");

        bottom.add(btnCancel);
        bottom.add(btnLogin);

        add(form, BorderLayout.CENTER);
        add(lblError, BorderLayout.NORTH);
        add(bottom, BorderLayout.SOUTH);

        // Cancel closes dialog
        btnCancel.addActionListener(e -> dispose());

        // Login button logic
        btnLogin.addActionListener(e -> {
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());

            User user = backend.login(username, password);

            if (user != null) {
                authenticatedUser = user;
                dispose();
            } else {
                lblError.setText("Invalid username or password.");
            }
        });
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}
