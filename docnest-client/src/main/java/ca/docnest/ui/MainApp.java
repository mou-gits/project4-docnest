package ca.docnest.ui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * @class MainApp
 * @brief Entry point for the DocNest JavaFX client application.
 *
 * @details
 * The {@code MainApp} class is the main launcher class for the DocNest user
 * interface. It extends {@link Application}, which is the standard base class
 * required for JavaFX applications.
 *
 * The JavaFX runtime automatically calls the {@link #start(Stage)} method
 * after the application environment has been initialized.
 *
 * Responsibilities of this class include:
 * <ul>
 *   <li>Launching the JavaFX runtime</li>
 *   <li>Creating the primary application window</li>
 *   <li>Initializing and displaying the main user interface</li>
 * </ul>
 *
 * The actual UI logic and layout are delegated to the {@link MainView} class.
 *
 * @see Application
 * @see MainView
 */
public class MainApp extends Application {

    /**
     * @brief Program entry point.
     *
     * @details
     * This method is executed when the application is started from the command
     * line or an executable launcher.
     *
     * It delegates startup control to the JavaFX runtime by calling
     * {@link #launch(String...)}. The runtime then performs initialization and
     * invokes the {@link #start(Stage)} lifecycle method.
     *
     * @param args Command-line arguments passed to the application.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * @brief Initializes and displays the main application window.
     *
     * @details
     * This lifecycle method is automatically called by the JavaFX framework
     * after startup initialization has completed.
     *
     * It creates an instance of {@link MainView}, passing in the primary stage,
     * and then displays the main user interface by calling its
     * {@code show()} method.
     *
     * The provided {@code primaryStage} acts as the main window of the
     * application.
     *
     * @param primaryStage The primary JavaFX stage supplied by the runtime.
     */
    @Override
    public void start(Stage primaryStage) {
        new MainView(primaryStage).show();
    }
}