package gui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import logic.ApplicationController;
import logic.ReminderService;
import storage.FileStorage;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controls the calendar view of the application.
 * Displays interview dates, application deadlines, and reminders on a monthly grid.
 */
public class CalendarController {

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;

    private YearMonth currentMonth;

    private final FileStorage fileStorage = new FileStorage();
    private final ApplicationController appController  = new ApplicationController(fileStorage);
    private final ReminderService       reminderService = new ReminderService(fileStorage);

    /** Maps each date to a list of [label, hexColor] pairs for rendering badges. */
    private Map<LocalDate, List<String[]>> eventMap;

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        loadEvents();
        buildCalendar();
    }

    @FXML
    private void handlePrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        buildCalendar();
    }

    @FXML
    private void handleNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        buildCalendar();
    }

    private void loadEvents() {
        eventMap = new HashMap<>();

        // Deadlines — red
        appController.getAllApplications().stream()
                .filter(a -> a.getDeadline() != null)
                .forEach(a -> addEvent(a.getDeadline(), a.getCompanyName() + " deadline", "#f87171"));

        // Interviews — blue
        Map<String, String> appIdToCompany = appController.getAllApplications().stream()
                .collect(Collectors.toMap(
                        logic.Application::getId,
                        logic.Application::getCompanyName,
                        (a, b) -> a));

        fileStorage.loadAllInterviews().forEach(i -> {
            String company = appIdToCompany.getOrDefault(i.getApplicationId(), "Interview");
            addEvent(i.getDate().toLocalDate(), "R" + i.getRound() + ": " + company, "#60a5fa");
        });

        // Reminders — amber
        reminderService.getUpcomingReminders(365).forEach(r ->
                addEvent(r.getTriggerDate(), r.getType().name(), "#fbbf24"));
    }

    private void addEvent(LocalDate date, String label, String color) {
        eventMap.computeIfAbsent(date, d -> new ArrayList<>())
                .add(new String[]{label, color});
    }

    private void buildCalendar() {
        monthYearLabel.setText(
                currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                        + " " + currentMonth.getYear());

        calendarGrid.getChildren().clear();

        int startOffset = currentMonth.atDay(1).getDayOfWeek().getValue() % 7;
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int day = 1;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                VBox cell = new VBox(2);
                cell.setPadding(new Insets(4));
                cell.setMinHeight(90);

                int cellIndex = row * 7 + col;
                boolean inMonth = cellIndex >= startOffset && day <= daysInMonth;

                if (inMonth) {
                    LocalDate cellDate = currentMonth.atDay(day);
                    boolean isToday = cellDate.equals(today);

                    cell.getStyleClass().add("calendar-cell");
                    if (isToday) cell.getStyleClass().add("calendar-cell-today");

                    Label dayLabel = new Label(String.valueOf(day));
                    dayLabel.getStyleClass().add("calendar-day-label");
                    if (isToday) dayLabel.getStyleClass().add("calendar-day-label-today");
                    cell.getChildren().add(dayLabel);

                    // Event badges — colors are data-driven so inline style stays
                    for (String[] event : eventMap.getOrDefault(cellDate, Collections.emptyList())) {
                        Label badge = new Label(truncate(event[0], 15));
                        badge.setMaxWidth(Double.MAX_VALUE);
                        badge.setPadding(new Insets(1, 4, 1, 4));
                        badge.setStyle(
                                "-fx-background-color: " + event[1] + "22;"
                                        + "-fx-text-fill: "       + event[1] + ";"
                                        + "-fx-font-size: 10px;"
                                        + "-fx-background-radius: 3;");
                        cell.getChildren().add(badge);
                    }
                    day++;
                } else {
                    cell.getStyleClass().add("calendar-cell-empty");
                }

                calendarGrid.add(cell, col, row);
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}