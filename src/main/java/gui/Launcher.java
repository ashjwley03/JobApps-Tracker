package gui;

import javafx.application.Application;

/**
 * Launches the JavaFX application.
 * Acts as the entry point for the GUI by delegating control to the Main class.
 */
public class Launcher {

    /**
     * Starts the application by launching the JavaFX runtime.
     *
     * @param args Command-line arguments passed to the application.
     */
    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}