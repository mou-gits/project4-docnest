package ca.docnest.ui;

import ca.docnest.backend.BackendFake;
import ca.docnest.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginDialogFX {

    private final BackendFake backend;

    private User authenticatedUser = null;

    public LoginDialogFX(BackendFake backend) {
        this.backend = backend;
    }

    public User showAndWait() {
        Stage stage = new Stage();
        stage.setTitle("Login");
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

        grid.add(lblError, 0, 2, 2, 1);

        grid.add(buttonBox, 0, 3, 2, 1);

        btnCancel.setOnAction(e -> stage.close());

        btnLogin.setOnAction(e -> {
            String username = txtUsername.getText().trim();
            String password = txtPassword.getText();

            User user = backend.login(username, password);

            if (user != null) {
                authenticatedUser = user;
                stage.close();
            } else {
                lblError.setText("Invalid username or password.");
            }
        });

        Scene scene = new Scene(grid, 350, 200);
        stage.setScene(scene);
        stage.showAndWait();

        return authenticatedUser;
    }
}
