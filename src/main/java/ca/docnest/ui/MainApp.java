package ca.docnest.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView view = new MainView();

        Scene scene = new Scene(view.getRoot(), 800, 600);

        primaryStage.setTitle("DocNest Client (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
