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

    // Shared FileStorage instance — used directly for interviews since
    // InterviewController does not expose a getAllInterviews() method.
    private final FileStorage fileStorage = new FileStorage();
    private final ApplicationController appController  = new ApplicationController(fileStorage);
    private final ReminderService       reminderService = new ReminderService(fileStorage);

    /** Maps each date to a list of [label, cssColor] pairs for rendering badges. */
    private Map<LocalDate, List<String[]>> eventMap;

    /**
     * Initializes the calendar after the FXML has been loaded.
     * Loads all events and renders the current month.
     */
    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        loadEvents();
        buildCalendar();
    }

    /** Navigates to the previous month and rebuilds the grid. */
    @FXML
    private void handlePrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        buildCalendar();
    }

    /** Navigates to the next month and rebuilds the grid. */
    @FXML
    private void handleNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        buildCalendar();
    }

    /**
     * Loads all events from storage into the event map.
     * Covers application deadlines (red), interview dates (blue), and reminders (amber).
     */
    private void loadEvents() {
        eventMap = new HashMap<>();

        // Application deadlines (red)
        appController.getAllApplications().stream()
                .filter(a -> a.getDeadline() != null)
                .forEach(a -> addEvent(
                        a.getDeadline(),
                        a.getCompanyName() + " deadline",
                        "#ef4444"));

        // Interview dates — cross-reference applicationId → company name (blue)
        Map<String, String> appIdToCompany = appController.getAllApplications().stream()
                .collect(Collectors.toMap(
                        logic.Application::getId,
                        logic.Application::getCompanyName,
                        (a, b) -> a));

        fileStorage.loadAllInterviews().forEach(i -> {
            String company = appIdToCompany.getOrDefault(i.getApplicationId(), "Interview");
            addEvent(
                    i.getDate().toLocalDate(),
                    "R" + i.getRound() + ": " + company,
                    "#2563eb");
        });

        // Reminders — fetch up to a year out (amber)
        reminderService.getUpcomingReminders(365).forEach(r ->
                addEvent(
                        r.getTriggerDate(),
                        r.getType().name(),
                        "#f59e0b"));
    }

    private void addEvent(LocalDate date, String label, String color) {
        eventMap.computeIfAbsent(date, d -> new ArrayList<>())
                .add(new String[]{label, color});
    }

    /**
     * Rebuilds the calendar grid for the current month.
     * Highlights today and renders color-coded event badges on each day cell.
     */
    private void buildCalendar() {
        monthYearLabel.setText(
                currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + currentMonth.getYear());

        calendarGrid.getChildren().clear();

        // Sunday = 0 offset; DayOfWeek.SUNDAY.getValue() = 7, so mod 7 = 0
        int startOffset  = currentMonth.atDay(1).getDayOfWeek().getValue() % 7;
        int daysInMonth  = currentMonth.lengthOfMonth();
        LocalDate today  = LocalDate.now();

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

                    cell.setStyle(
                            "-fx-border-color: #e5e7eb; -fx-border-width: 0.5;"
                            + (isToday ? " -fx-background-color: #eff6ff;" : " -fx-background-color: white;"));

                    Label dayLabel = new Label(String.valueOf(day));
                    dayLabel.setStyle(
                            "-fx-font-size: 13px; -fx-font-weight: bold;"
                            + (isToday ? " -fx-text-fill: #2563eb;" : " -fx-text-fill: #111827;"));
                    cell.getChildren().add(dayLabel);

                    // Render event badges
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
                    cell.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 0.5; -fx-background-color: #f9fafb;");
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
