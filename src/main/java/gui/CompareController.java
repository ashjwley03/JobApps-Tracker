package gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxListCell;
import logic.Application;
import logic.ApplicationController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controls the compare view of the application.
 * Lets users select applications via checkboxes and compare them side by side.
 */
public class CompareController {

    @FXML private ListView<Application> appListView;
    @FXML private TableView<Application> compareTable;
    @FXML private TableColumn<Application, String> colCompany;
    @FXML private TableColumn<Application, String> colRole;
    @FXML private TableColumn<Application, String> colPay;
    @FXML private TableColumn<Application, String> colLocation;
    @FXML private TableColumn<Application, String> colStatus;
    @FXML private TableColumn<Application, String> colDeadline;
    @FXML private Label hintLabel;

    private ApplicationController appController;

    private final Map<String, BooleanProperty> checkedState = new LinkedHashMap<>();
    private final ObservableList<Application> allApps      = FXCollections.observableArrayList();
    private final ObservableList<Application> selectedApps = FXCollections.observableArrayList();

    /**
     * Sets the ApplicationController used to load and compare applications.
     * Must be called by MainController before the view is displayed.
     *
     * @param appController The application controller to use.
     */
    public void setAppController(ApplicationController appController) {
        this.appController = appController;
    }

    /**
     * Initialises the controller after the FXML has been loaded.
     * Sets up the comparison table columns, loads all applications, and configures the checkbox list.
     */
    @FXML
    public void initialize() {
        setupTable();
    }

    /**
     * Loads application data and configures the list view after dependencies have been injected.
     * Called by MainController immediately after setAppController.
     */
    public void loadData() {
        loadApplications();
        setupListView();
    }

    private void setupTable() {
        colCompany.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCompanyName()));
        colRole.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRoleTitle()));
        colPay.setCellValueFactory(c -> {
            double pay = c.getValue().getPay();
            return new SimpleStringProperty(pay > 0 ? String.format("$%.0f", pay) : "—");
        });
        colLocation.setCellValueFactory(c -> {
            String loc = c.getValue().getLocation();
            return new SimpleStringProperty(loc != null && !loc.isBlank() ? loc : "—");
        });
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().name()));
        colDeadline.setCellValueFactory(c -> {
            var d = c.getValue().getDeadline();
            return new SimpleStringProperty(d != null ? d.toString() : "—");
        });

        // Highlight the highest-pay row in orange (dark-theme friendly)
        compareTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Application app, boolean empty) {
                super.updateItem(app, empty);
                if (empty || app == null) {
                    setStyle("");
                } else {
                    boolean isTop = !selectedApps.isEmpty()
                            && selectedApps.stream()
                            .max(Comparator.comparingDouble(Application::getPay))
                            .filter(a -> a.getId().equals(app.getId()))
                            .isPresent()
                            && app.getPay() > 0;
                    setStyle(isTop ? "-fx-background-color: #f9731620;" : ""); // Dark orange tint for best-pay row
                }
            }
        });

        compareTable.setItems(selectedApps);
        compareTable.setPlaceholder(new Label("Select applications on the left to compare."));
    }

    private void loadApplications() {
        List<Application> apps = appController.getAllApplications();
        allApps.setAll(apps);
        apps.forEach(a -> {
            BooleanProperty checked = new SimpleBooleanProperty(false);
            checked.addListener((obs, wasChecked, isNowChecked) -> updateComparison());
            checkedState.put(a.getId(), checked);
        });
    }

    private void setupListView() {
        appListView.setItems(allApps);
        appListView.setCellFactory(CheckBoxListCell.forListView(
                app -> checkedState.getOrDefault(app.getId(), new SimpleBooleanProperty(false)),
                new javafx.util.StringConverter<Application>() {
                    @Override public String toString(Application a) {
                        return a == null ? "" : a.getCompanyName() + "  —  " + a.getRoleTitle();
                    }
                    @Override public Application fromString(String s) { return null; }
                }
        ));

        if (allApps.isEmpty()) {
            hintLabel.setText("No applications found. Add some from the Dashboard first.");
        } else {
            hintLabel.setText("Check applications to compare. Highest pay is highlighted.");
        }
    }

    private void updateComparison() {
        List<String> checkedIds = checkedState.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (checkedIds.isEmpty()) {
            selectedApps.clear();
            return;
        }

        List<Application> compared = appController.compareApplications(checkedIds);
        selectedApps.setAll(compared);
        compareTable.refresh();
    }
}