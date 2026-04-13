package gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import logic.Application;
import logic.ApplicationController;
import logic.ApplicationStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Controls the dashboard view.
 * Populates stat cards, bar chart, pie chart, and the application table.
 */
public class DashboardController {

    private static final String STYLE_STATUS_OFFER        = "status-offer";
    private static final String STYLE_STATUS_INTERVIEWING = "status-interviewing";
    private static final String STYLE_STATUS_REJECTED     = "status-rejected";
    private static final String STYLE_STATUS_DEFAULT      = "status-default";

    @FXML private Label statTotal;
    @FXML private Label statApplied;
    @FXML private Label statInterviewing;
    @FXML private Label statOffers;
    @FXML private Label statRejected;
    @FXML private BarChart<String, Number> statusBarChart;
    @FXML private CategoryAxis barXAxis;
    @FXML private NumberAxis barYAxis;
    @FXML private PieChart statusPieChart;
    @FXML private TextField searchField;
    @FXML private TableView<Application> applicationTable;
    @FXML private TableColumn<Application, String> colCompany;
    @FXML private TableColumn<Application, String> colRole;
    @FXML private TableColumn<Application, String> colStatus;
    @FXML private TableColumn<Application, String> colDeadline;

    private final ObservableList<Application> masterList = FXCollections.observableArrayList();
    private FilteredList<Application> filteredList;

    private ApplicationController appController;
    private Runnable onNewApplication;

    /**
     * Sets the ApplicationController used to load and manage applications.
     * Must be called by MainController before the view is displayed.
     *
     * @param appController The application controller to use.
     */
    public void setAppController(ApplicationController appController) {
        this.appController = appController;
    }

    /**
     * Registers a callback invoked when the user requests to add a new application.
     * Typically set by {@link MainController} after loading this view.
     *
     * @param onNewApplication Runnable to execute when the new-application button is clicked.
     */
    public void setOnNewApplication(Runnable onNewApplication) {
        this.onNewApplication = onNewApplication;
    }

    /**
     * Initialises the controller after the FXML has been loaded.
     * Configures the table columns and search filter.
     */
    @FXML
    public void initialize() {
        setupTable();
    }

    /**
     * Loads and displays application data after dependencies have been injected.
     * Called by MainController immediately after setAppController.
     * Displays an error dialog if the logic layer throws an unexpected exception.
     */
    public void loadData() {
        List<Application> apps;
        try {
            apps = appController.getAllApplications();
        } catch (RuntimeException e) {
            GuiUtils.showError("Could Not Load Applications", e.getMessage());
            return;
        }
        masterList.setAll(apps);
        populateStats(apps);
        populateCharts(apps);
    }

    /**
     * Returns whether the given application matches the search keyword.
     * Matches against company name and role title, case-insensitively.
     * Package-private to allow direct invocation from tests without requiring JavaFX.
     *
     * @param app     The application to test.
     * @param keyword The search keyword, already trimmed and lowercased.
     * @return True if the application matches or keyword is empty, false otherwise.
     */
    boolean matchesSearch(Application app, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return app.getCompanyName().toLowerCase().contains(lowerKeyword)
                || app.getRoleTitle().toLowerCase().contains(lowerKeyword);
    }

    private void setupTable() {
        colCompany.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleTitle"));
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().name()));
        colDeadline.setCellValueFactory(c -> {
            LocalDate d = c.getValue().getDeadline();
            return new SimpleStringProperty(d != null ? d.toString() : "—");
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll(STYLE_STATUS_OFFER, STYLE_STATUS_INTERVIEWING,
                        STYLE_STATUS_REJECTED, STYLE_STATUS_DEFAULT);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(value);
                    if (value.equals(ApplicationStatus.OFFER.name())) {
                        getStyleClass().add(STYLE_STATUS_OFFER);
                    } else if (value.equals(ApplicationStatus.INTERVIEWING.name())) {
                        getStyleClass().add(STYLE_STATUS_INTERVIEWING);
                    } else if (value.equals(ApplicationStatus.REJECTED.name())) {
                        getStyleClass().add(STYLE_STATUS_REJECTED);
                    } else {
                        getStyleClass().add(STYLE_STATUS_DEFAULT);
                    }
                }
            }
        });

        filteredList = new FilteredList<>(masterList, p -> true);
        applicationTable.setItems(filteredList);
    }

    private void populateStats(List<Application> apps) {
        statTotal.setText(String.valueOf(apps.size()));
        statApplied.setText(String.valueOf(
                apps.stream().filter(a -> a.getStatus() == ApplicationStatus.APPLIED).count()));
        statInterviewing.setText(String.valueOf(
                apps.stream().filter(a -> a.getStatus() == ApplicationStatus.INTERVIEWING).count()));
        statOffers.setText(String.valueOf(
                apps.stream().filter(a -> a.getStatus() == ApplicationStatus.OFFER).count()));
        statRejected.setText(String.valueOf(
                apps.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count()));
    }

    private void populateCharts(List<Application> apps) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = apps.stream().filter(a -> a.getStatus() == status).count();
            series.getData().add(new XYChart.Data<>(status.name(), count));
        }
        statusBarChart.getData().add(series);
        statusBarChart.setBarGap(3);
        statusBarChart.setCategoryGap(12);

        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = apps.stream().filter(a -> a.getStatus() == status).count();
            if (count > 0) {
                statusPieChart.getData().add(new PieChart.Data(status.name(), count));
            }
        }

        if (statusPieChart.getData().isEmpty()) {
            statusPieChart.getData().add(new PieChart.Data("No Data", 1));
        }
    }

    @FXML
    private void handleNewApplication() {
        if (onNewApplication != null) {
            onNewApplication.run();
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        filteredList.setPredicate(entry -> matchesSearch(entry, keyword));
    }
}