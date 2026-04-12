package ca.docnest.ui;

import ca.docnest.client.network.ClientNetwork;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginDialogFX {

    private final Stage owner;
    private ClientNetwork authenticatedClient;

    public LoginDialogFX(Stage owner) {
        this.owner = owner;
    }

    public ClientNetwork showAndWait() {
        Stage stage = new Stage();
        stage.setTitle("Login");
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label lblUsername = new Label("Username:");
        TextField txtUsername = new TextField();

        Label lblPassword = new Label("Password:");
        PasswordField txtPassword = new PasswordField();

        Label lblHost = new Label("Host:");
        TextField txtHost = new TextField("localhost");

        Label lblPort = new Label("Port:");
        TextField txtPort = new TextField("9090");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        Button btnLogin = new Button("Login");
        Button btnCancel = new Button("Cancel");

        HBox buttonBox = new HBox(10, btnCancel, btnLogin);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        grid.add(lblUsername, 0, 0);
        grid.add(txtUsername, 1, 0);
        grid.add(lblPassword, 0, 1);
        grid.add(txtPassword, 1, 1);
        grid.add(lblHost, 0, 2);
        grid.add(txtHost, 1, 2);
        grid.add(lblPort, 0, 3);
        grid.add(txtPort, 1, 3);
        grid.add(lblError, 0, 4, 2, 1);
        grid.add(buttonBox, 0, 5, 2, 1);

        btnCancel.setOnAction(e -> stage.close());

        btnLogin.setOnAction(e -> {
            String username = txtUsername.getText().trim();
            String password = txtPassword.getText();
            String host = txtHost.getText().trim();
            String portText = txtPort.getText().trim();

            if (username.isEmpty() || password.isEmpty() || host.isEmpty() || portText.isEmpty()) {
                lblError.setText("All fields are required.");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException ex) {
                lblError.setText("Port must be a number.");
                return;
            }

            try {
                ClientNetwork client = new ClientNetwork();
                client.connect(host, port);
                if (!client.login(username, password)) {
                    client.close();
                    lblError.setText("Login failed.");
                    return;
                }

                authenticatedClient = client;
                stage.close();
            } catch (Exception ex) {
                lblError.setText(ex.getMessage());
            }
        });

        stage.setScene(new Scene(grid, 380, 260));
        stage.showAndWait();

        return authenticatedClient;
    }
}
