package gui;

import logic.Application;
import logic.ApplicationController;
import logic.ApplicationStatus;
import logic.InMemoryStorage;
import logic.InterviewController;
import logic.ReminderService;
import logic.ReminderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the event-loading logic in CalendarController.
 * Each test calls loadEvents() directly (package-private) to verify that
 * deadlines, interviews, and reminders are correctly mapped to dates in the
 * event map, without requiring the JavaFX runtime.
 */
class CalendarControllerTest {

    private CalendarController controller;
    private ApplicationController appController;
    private InterviewController interviewController;
    private ReminderService reminderService;
    private InMemoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
        appController = new ApplicationController(storage);
        interviewController = new InterviewController(storage);
        reminderService = new ReminderService(storage);

        controller = new CalendarController();
        controller.setAppController(appController);
        controller.setInterviewController(interviewController);
        controller.setReminderService(reminderService);
    }

    /**
     * Verifies that when an application has a deadline set, loadEvents() maps
     * exactly one event to that deadline date.
     * The deadline is set after creation and persisted via updateApplication()
     * because addApplication() does not accept a deadline parameter.
     */
    @Test
    void loadEvents_applicationWithDeadline_appearsOnCorrectDate() {
        LocalDate deadline = LocalDate.now().plusDays(5);
        Application app = appController.addApplication(
                "Google", "SWE Intern", 5000, "SG", ApplicationStatus.APPLIED);
        app.setDeadline(deadline);
        storage.updateApplication(app);

        controller.loadEvents();

        assertEquals(1, controller.getEventCountForDate(deadline));
    }

    /**
     * Verifies that when an application has no deadline (null), loadEvents()
     * does not add any event to the event map. Applications without deadlines
     * should be silently skipped.
     */
    @Test
    void loadEvents_applicationWithNullDeadline_notAddedToEventMap() {
        // Add an application but do not set a deadline — deadline stays null
        appController.addApplication("Google", "SWE Intern", 5000, "SG", ApplicationStatus.APPLIED);

        controller.loadEvents();

        // Check a nearby date to confirm nothing was mapped
        assertEquals(0, controller.getEventCountForDate(LocalDate.now().plusDays(1)));
    }

    /**
     * Verifies that an interview is mapped to the date of the interview
     * (extracted from LocalDateTime) in the event map.
     */
    @Test
    void loadEvents_interview_appearsOnCorrectDate() {
        Application app = appController.addApplication(
                "Meta", "PM Intern", 4000, "SG", ApplicationStatus.INTERVIEWING);
        LocalDate interviewDate = LocalDate.now().plusDays(3);
        // atTime converts the LocalDate to a LocalDateTime as required by addInterview
        interviewController.addInterview(app.getId(), 1, interviewDate.atTime(10, 0));

        controller.loadEvents();

        assertEquals(1, controller.getEventCountForDate(interviewDate));
    }

    /**
     * Verifies that a reminder whose trigger date falls within the 365-day
     * look-ahead window is included in the event map.
     */
    @Test
    void loadEvents_reminderWithinWindow_included() {
        Application app = appController.addApplication(
                "TestCo", "Intern", 3000, "SG", ApplicationStatus.APPLIED);
        LocalDate triggerDate = LocalDate.now().plusDays(100);
        reminderService.addReminder(app.getId(), ReminderType.DEADLINE, triggerDate);

        controller.loadEvents();

        assertEquals(1, controller.getEventCountForDate(triggerDate));
    }

    /**
     * Verifies that a reminder whose trigger date falls beyond the 365-day
     * look-ahead window is excluded from the event map entirely.
     * This ensures CalendarController does not show events too far in the future.
     */
    @Test
    void loadEvents_reminderBeyondWindow_excluded() {
        Application app = appController.addApplication(
                "TestCo", "Intern", 3000, "SG", ApplicationStatus.APPLIED);
        LocalDate triggerDate = LocalDate.now().plusDays(400);
        reminderService.addReminder(app.getId(), ReminderType.FOLLOWUP, triggerDate);

        controller.loadEvents();

        assertEquals(0, controller.getEventCountForDate(triggerDate));
    }

    /**
     * Verifies that when a deadline and an interview fall on the same date,
     * both events are mapped to that date and counted correctly.
     */
    @Test
    void loadEvents_multipleEventsOnSameDate_allCounted() {
        LocalDate sharedDate = LocalDate.now().plusDays(7);

        Application app = appController.addApplication(
                "Grab", "Backend Intern", 3500, "SG", ApplicationStatus.INTERVIEWING);
        app.setDeadline(sharedDate);
        storage.updateApplication(app);

        // Interview on the same day as the deadline
        interviewController.addInterview(app.getId(), 1, sharedDate.atTime(14, 0));

        controller.loadEvents();

        // Both the deadline event and the interview event should appear on this date
        assertEquals(2, controller.getEventCountForDate(sharedDate));
    }
}
