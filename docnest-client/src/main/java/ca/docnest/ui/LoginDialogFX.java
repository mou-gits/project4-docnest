package ca.docnest.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @class LoginDialogFX
 * @brief Provides a JavaFX modal login dialog for collecting user credentials.
 *
 * @details
 * The {@code LoginDialogFX} class displays a modal window that prompts the
 * user to enter a username and password before accessing secured features of
 * the DocNest application.
 *
 * The dialog includes:
 * <ul>
 *   <li>A text field for entering the username</li>
 *   <li>A password field for entering the password</li>
 *   <li>A login button to confirm input</li>
 *   <li>A cancel button to dismiss the dialog</li>
 *   <li>An error label for validation messages</li>
 * </ul>
 *
 * The dialog blocks interaction with the parent window until closed because
 * it uses {@code Modality.APPLICATION_MODAL}.
 *
 * After the dialog closes, the entered values can be retrieved using
 * {@link #getUsername()} and {@link #getPassword()}.
 *
 * If the user cancels the dialog, both values are set to {@code null}.
 */
public class LoginDialogFX {

    /**
     * @brief Owner stage of the login dialog.
     *
     * @details
     * If provided, the login dialog is attached to this parent window so that
     * it behaves as a child modal dialog.
     */
    private final Stage owner;

    /**
     * @brief Stores the username entered by the user.
     *
     * @details
     * This field is populated when the user successfully submits the login
     * form. It is set to {@code null} if the dialog is cancelled.
     */
    private String username;

    /**
     * @brief Stores the password entered by the user.
     *
     * @details
     * This field is populated when the user successfully submits the login
     * form. It is set to {@code null} if the dialog is cancelled.
     */
    private String password;

    /**
     * @brief Constructs a login dialog associated with an owner window.
     *
     * @details
     * Initializes the dialog with the specified parent stage. If the owner is
     * not {@code null}, the login window will be centered relative to it and
     * behave as its child modal dialog.
     *
     * @param owner The parent {@link Stage} that owns this dialog. May be
     *              {@code null} if no owner window is required.
     */
    public LoginDialogFX(Stage owner) {
        this.owner = owner;
    }

    /**
     * @brief Returns the username entered by the user.
     *
     * @details
     * This method should be called after {@link #showAndWait()} has completed.
     * If the dialog was cancelled, the result will be {@code null}.
     *
     * @return The entered username, or {@code null} if cancelled.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @brief Returns the password entered by the user.
     *
     * @details
     * This method should be called after {@link #showAndWait()} has completed.
     * If the dialog was cancelled, the result will be {@code null}.
     *
     * @return The entered password, or {@code null} if cancelled.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @brief Displays the login dialog and waits until the user closes it.
     *
     * @details
     * This method creates the full JavaFX user interface for the login form,
     * configures the modal dialog window, and handles button events.
     *
     * Workflow:
     * <ol>
     *   <li>Create a new modal stage</li>
     *   <li>Add username and password input fields</li>
     *   <li>Add login and cancel buttons</li>
     *   <li>Validate that both fields are filled in</li>
     *   <li>Store credentials if valid</li>
     *   <li>Close the dialog after login or cancellation</li>
     * </ol>
     *
     * Validation rules:
     * <ul>
     *   <li>Username must not be empty</li>
     *   <li>Password must not be empty</li>
     * </ul>
     *
     * If validation fails, an error message is shown and the dialog remains
     * open until corrected or cancelled.
     */
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