package ca.docnest.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginDialogFX {

    private final Stage owner;

    private String username;
    private String password;

    public LoginDialogFX(Stage owner) {
        this.owner = owner;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void showAndWait() {
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

        TextField txtUsername = new TextField();
        PasswordField txtPassword = new PasswordField();

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        Button btnLogin = new Button("Login");
        Button btnCancel = new Button("Cancel");

        HBox buttonBox = new HBox(10, btnCancel, btnLogin);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        grid.add(new Label("Username:"), 0, 0);
        grid.add(txtUsername, 1, 0);

        grid.add(new Label("Password:"), 0, 1);
        grid.add(txtPassword, 1, 1);

        grid.add(lblError, 0, 2, 2, 1);
        grid.add(buttonBox, 0, 3, 2, 1);

        btnCancel.setOnAction(e -> {
            username = null;
            password = null;
            stage.close();
        });

        btnLogin.setOnAction(e -> {
            String u = txtUsername.getText().trim();
            String p = txtPassword.getText();

            if (u.isEmpty() || p.isEmpty()) {
                lblError.setText("Username and password required.");
                return;
            }

            username = u;
            password = p;
            stage.close();
        });

        stage.setScene(new Scene(grid, 300, 180));
        stage.showAndWait();
    }
}