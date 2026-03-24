package logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReminderServiceTest {

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(new InMemoryStorage());
    }

    @Test
    void addReminder_validInput_storesReminder() {
        Reminder reminder = reminderService.addReminder(
                "app-123", ReminderType.DEADLINE, LocalDate.now().plusDays(2));
        assertNotNull(reminder);
        assertFalse(reminder.isDismissed());
    }

    @Test
    void getUpcomingReminders_withinWindow_returnsCorrectReminders() {
        reminderService.addReminder("app-1", ReminderType.DEADLINE, LocalDate.now().plusDays(1));
        reminderService.addReminder("app-2", ReminderType.INTERVIEW, LocalDate.now().plusDays(3));
        reminderService.addReminder("app-3", ReminderType.FOLLOWUP, LocalDate.now().plusDays(10));
        List<Reminder> upcoming = reminderService.getUpcomingReminders(5);
        assertEquals(2, upcoming.size());
    }

    @Test
    void getUpcomingReminders_dismissedReminderExcluded() {
        Reminder r = reminderService.addReminder(
                "app-1", ReminderType.DEADLINE, LocalDate.now().plusDays(1));
        reminderService.dismissReminder(r.getId());
        List<Reminder> upcoming = reminderService.getUpcomingReminders(5);
        assertTrue(upcoming.isEmpty());
    }

    @Test
    void getUpcomingReminders_sortedByDateAscending() {
        reminderService.addReminder("app-1", ReminderType.DEADLINE, LocalDate.now().plusDays(3));
        reminderService.addReminder("app-2", ReminderType.INTERVIEW, LocalDate.now().plusDays(1));
        List<Reminder> upcoming = reminderService.getUpcomingReminders(5);
        assertTrue(upcoming.get(0).getTriggerDate().isBefore(upcoming.get(1).getTriggerDate()));
    }
}