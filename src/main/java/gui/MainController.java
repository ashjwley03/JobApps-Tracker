package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;

/**
 * Controls the main window of the application.
 * Handles navigation between views and highlights the active sidebar button.
 */
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnCalendar;
    @FXML private Button btnCompare;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        navButtons = List.of(btnDashboard, btnCalendar, btnCompare);
        // Apply default style — FXML doesn't set styleClass on these buttons
        // so we initialise them all as inactive here.
        navButtons.forEach(b -> b.getStyleClass().setAll("nav-button"));
        showDashboard();
    }
    private void setActive(Button active) {
        for (Button btn : navButtons) {
            btn.getStyleClass().setAll(btn == active ? "nav-button-active" : "nav-button");
        }
    }

    @FXML
    public void showDashboard() {
        setActive(btnDashboard);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DashboardView.fxml"));
            Node view = loader.load();
            DashboardController controller = loader.getController();
            controller.setOnNewApplication(this::showNewApplication);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: /view/DashboardView.fxml", e);
        }
    }

    private void showNewApplication() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/NewApplicationView.fxml"));
            Node view = loader.load();
            NewApplicationController controller = loader.getController();
            controller.setOnSuccess(this::showDashboard);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: /view/NewApplicationView.fxml", e);
        }
    }

    @FXML
    private void showCalendar() {
        setActive(btnCalendar);
        loadView("/view/CalendarView.fxml");
    }

    @FXML
    private void showCompare() {
        setActive(btnCompare);
        loadView("/view/CompareView.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, e);
        }
    }
}