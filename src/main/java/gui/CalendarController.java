package gui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import logic.ApplicationController;
import logic.InterviewController;
import logic.ReminderService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controls the calendar view of the application.
 * Displays interview dates, application deadlines, and reminders on a monthly grid.
 */
public class CalendarController {

    private static final String BADGE_DEADLINE  = "badge-deadline";
    private static final String BADGE_INTERVIEW = "badge-interview";
    private static final String BADGE_REMINDER  = "badge-reminder";

    private static final int BADGE_TRUNCATE_LENGTH = 15;
    private static final int CALENDAR_ROWS         = 6;
    private static final int CALENDAR_COLS         = 7;
    private static final int CELL_MIN_HEIGHT       = 90;
    private static final int REMINDER_LOOK_AHEAD   = 365;

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;

    private YearMonth currentMonth;

    private ApplicationController appController;
    private InterviewController interviewController;
    private ReminderService reminderService;

    /** Maps each date to a list of event badges to render in the calendar cell. */
    private Map<LocalDate, List<CalendarEventBadge>> eventMap;

    /**
     * Sets the ApplicationController used to load application deadlines.
     * Must be called by MainController before the view is displayed.
     *
     * @param appController The application controller to use.
     */
    public void setAppController(ApplicationController appController) {
        this.appController = appController;
    }

    /**
     * Sets the InterviewController used to load interview dates.
     * Must be called by MainController before the view is displayed.
     *
     * @param interviewController The interview controller to use.
     */
    public void setInterviewController(InterviewController interviewController) {
        this.interviewController = interviewController;
    }

    /**
     * Sets the ReminderService used to load upcoming reminders.
     * Must be called by MainController before the view is displayed.
     *
     * @param reminderService The reminder service to use.
     */
    public void setReminderService(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * Initialises the controller after the FXML has been loaded.
     * Sets the displayed month to the current month.
     */
    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
    }

    /**
     * Loads event data and renders the calendar grid.
     * Called by MainController immediately after all dependencies have been injected.
     * Displays an error dialog if the logic layer throws an unexpected exception.
     */
    public void loadData() {
        try {
            loadEvents();
        } catch (RuntimeException e) {
            GuiUtils.showError("Could Not Load Calendar Data", e.getMessage());
            eventMap = new HashMap<>();
        }
        buildCalendar();
    }

    /**
     * Returns the number of events mapped to the given date.
     * Package-private to allow access from tests without requiring JavaFX.
     *
     * @param date The date to check.
     * @return Number of events on that date.
     */
    int getEventCountForDate(LocalDate date) {
        if (eventMap == null) {
            return 0;
        }
        return eventMap.getOrDefault(date, Collections.emptyList()).size();
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

    /**
     * Builds the event map from all deadlines, interviews, and reminders.
     * Package-private to allow direct invocation from tests without requiring JavaFX.
     * Throws IllegalArgumentException or IllegalStateException if any logic call fails —
     * callers are responsible for catching and displaying these.
     */
    void loadEvents() {
        eventMap = new HashMap<>();

        // Deadlines — sourced from all applications that have a deadline set
        appController.getAllApplications().stream()
                .filter(a -> a.getDeadline() != null)
                .forEach(a -> addEvent(a.getDeadline(),
                        a.getCompanyName() + " deadline", BADGE_DEADLINE));

        // Build a lookup map from application ID to company name for interview labelling
        Map<String, String> appIdToCompany = appController.getAllApplications().stream()
                .collect(Collectors.toMap(
                        logic.Application::getId,
                        logic.Application::getCompanyName,
                        (a, b) -> a));

        // Interviews — accessed through InterviewController to respect the logic layer
        interviewController.getAllInterviews().forEach(i -> {
            String company = appIdToCompany.getOrDefault(i.getApplicationId(), "Interview");
            addEvent(i.getDate().toLocalDate(),
                    "R" + i.getRound() + ": " + company, BADGE_INTERVIEW);
        });

        // Reminders — only those falling within the look-ahead window
        reminderService.getUpcomingReminders(REMINDER_LOOK_AHEAD).forEach(r ->
                addEvent(r.getTriggerDate(), r.getType().name(), BADGE_REMINDER));
    }

    private void addEvent(LocalDate date, String label, String styleClass) {
        eventMap.computeIfAbsent(date, d -> new ArrayList<>())
                .add(new CalendarEventBadge(label, styleClass));
    }

    private void buildCalendar() {
        monthYearLabel.setText(
                currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                        + " " + currentMonth.getYear());

        calendarGrid.getChildren().clear();

        // Offset so the first day of the month lands on the correct column (Sunday = 0)
        int startOffset = currentMonth.atDay(1).getDayOfWeek().getValue() % CALENDAR_COLS;
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int day = 1;
        for (int row = 0; row < CALENDAR_ROWS; row++) {
            for (int col = 0; col < CALENDAR_COLS; col++) {
                VBox cell = new VBox(2);
                cell.setPadding(new Insets(4));
                cell.setMinHeight(CELL_MIN_HEIGHT);

                int cellIndex = row * CALENDAR_COLS + col;
                boolean inMonth = cellIndex >= startOffset && day <= daysInMonth;

                if (inMonth) {
                    LocalDate cellDate = currentMonth.atDay(day);
                    boolean isToday = cellDate.equals(today);

                    cell.getStyleClass().add("calendar-cell");
                    if (isToday) {
                        cell.getStyleClass().add("calendar-cell-today");
                    }

                    Label dayLabel = new Label(String.valueOf(day));
                    dayLabel.getStyleClass().add("calendar-day-label");
                    if (isToday) {
                        dayLabel.getStyleClass().add("calendar-day-label-today");
                    }
                    cell.getChildren().add(dayLabel);

                    // Render one badge label per event on this date
                    for (CalendarEventBadge event : eventMap.getOrDefault(cellDate, Collections.emptyList())) {
                        Label badge = new Label(truncate(event.label(), BADGE_TRUNCATE_LENGTH));
                        badge.setMaxWidth(Double.MAX_VALUE);
                        badge.setPadding(new Insets(1, 4, 1, 4));
                        badge.getStyleClass().add(event.styleClass());
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
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * Represents a single event badge rendered inside a calendar day cell.
     *
     * @param label      Display text shown on the badge.
     * @param styleClass CSS class applied to the badge for colour and styling.
     */
    private record CalendarEventBadge(String label, String styleClass) {}
}