package ca.docnest.ui;

import ca.docnest.client.network.ClientNetwork;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        new MainView(primaryStage).show();
    }
}
