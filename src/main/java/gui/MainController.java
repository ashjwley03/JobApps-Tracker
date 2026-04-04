package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import logic.ApplicationController;
import logic.ReminderService;
import storage.FileStorage;

import java.io.IOException;
import java.util.List;

/**
 * Controls the main window of the application.
 * Handles navigation between views and highlights the active sidebar button.
 * Owns the single instances of FileStorage, ApplicationController, and ReminderService
 * that are injected into all child controllers.
 */
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnCalendar;
    @FXML private Button btnCompare;

    private List<Button> navButtons;

    private final FileStorage fileStorage = new FileStorage();
    private final ApplicationController appController = new ApplicationController(fileStorage);
    private final ReminderService reminderService = new ReminderService(fileStorage);

    /**
     * Initialises the controller after the FXML has been loaded.
     * Resets all navigation buttons to their inactive style and shows the dashboard view.
     */
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

    /**
     * Navigates to the dashboard view and marks its sidebar button as active.
     * Also wires the new-application callback so the dashboard can trigger navigation.
     */
    @FXML
    public void showDashboard() {
        setActive(btnDashboard);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DashboardView.fxml"));
            Node view = loader.load();
            DashboardController controller = loader.getController();
            controller.setAppController(appController);
            controller.setOnNewApplication(this::showNewApplication);
            controller.loadData();
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
            controller.setAppController(appController);
            controller.setOnSuccess(this::showDashboard);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: /view/NewApplicationView.fxml", e);
        }
    }

    @FXML
    private void showCalendar() {
        setActive(btnCalendar);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CalendarView.fxml"));
            Node view = loader.load();
            CalendarController controller = loader.getController();
            controller.setAppController(appController);
            controller.setReminderService(reminderService);
            controller.setFileStorage(fileStorage);
            controller.loadData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: /view/CalendarView.fxml", e);
        }
    }

    @FXML
    private void showCompare() {
        setActive(btnCompare);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CompareView.fxml"));
            Node view = loader.load();
            CompareController controller = loader.getController();
            controller.setAppController(appController);
            controller.loadData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: /view/CompareView.fxml", e);
        }
    }
}