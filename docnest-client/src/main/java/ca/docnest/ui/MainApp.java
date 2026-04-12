package ca.docnest.ui;

import ca.docnest.client.network.ClientNetwork;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        LoginDialogFX loginDialog = new LoginDialogFX(primaryStage);
        ClientNetwork client = loginDialog.showAndWait();

        if (client == null) {
            primaryStage.close();
            return;
        }

        primaryStage.setOnCloseRequest(event -> {
            try {
                client.logout();
            } catch (Exception ignored) {
                client.close();
            }
        });

        new MainView(primaryStage, client).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
