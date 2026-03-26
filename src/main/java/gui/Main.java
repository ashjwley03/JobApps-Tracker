package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Represents the main JavaFX application.
 * Responsible for loading the main window layout and displaying the primary stage.
 */
public class Main extends Application {

    /**
     * Loads the main FXML layout, sets up the scene, and displays the primary stage.
     */
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/view/MainWindow.fxml"));
            BorderPane ap = fxmlLoader.load();
            Scene scene = new Scene(ap, 1050, 680);
            scene.getStylesheets().add(
                    Main.class.getResource("/view/styles.css").toExternalForm()
            );
            stage.setScene(scene);
            stage.setTitle("Job Application Tracker");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}