package gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import logic.Application;
import logic.ApplicationController;
import logic.ApplicationStatus;
import storage.FileStorage;

import java.util.List;

/**
 * Controls the dashboard view.
 * Populates stat cards, bar chart, pie chart, and the application table.
 */
public class DashboardController {

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

    private final ApplicationController appController =
            new ApplicationController(new FileStorage());

    private Runnable onNewApplication;

    public void setOnNewApplication(Runnable onNewApplication) {
        this.onNewApplication = onNewApplication;
    }

    @FXML
    public void initialize() {
        setupTable();
        List<Application> apps = appController.getAllApplications();
        masterList.setAll(apps);
        populateStats(apps);
        populateCharts(apps);
    }

    private void setupTable() {
        colCompany.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleTitle"));
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().name()));
        colDeadline.setCellValueFactory(c -> {
            var d = c.getValue().getDeadline();
            return new SimpleStringProperty(d != null ? d.toString() : "—");
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(value);
                    if (value.equals("OFFERED")) {
                        setStyle("-fx-text-fill: #f97316; -fx-font-weight: bold;");
                    } else if (value.equals("INTERVIEWING")) {
                        setStyle("-fx-text-fill: #dd6b20;");
                    } else if (value.equals("REJECTED")) {
                        setStyle("-fx-text-fill: #f87171;");
                    } else {
                        setStyle("-fx-text-fill: #d1d5db;");
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
        if (onNewApplication != null) onNewApplication.run();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        filteredList.setPredicate(entry ->
                keyword.isEmpty()
                        || entry.getCompanyName().toLowerCase().contains(keyword)
                        || entry.getRoleTitle().toLowerCase().contains(keyword));
    }
}