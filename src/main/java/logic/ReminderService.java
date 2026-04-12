package logic;

import storage.Storage;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages follow-up reminders and deadlines.
 * Ensures that reminders are correctly linked to existing applications
 * and provides logic for querying and dismissing upcoming alerts.
 */
public class ReminderService {
    private final Storage storage;

    /**
     * Constructs a ReminderService with a specified storage dependency.
     *
     * @param storage The storage implementation used for data persistence.
     */
    public ReminderService(Storage storage) {
        this.storage = storage;
    }

    /**
     * Creates and saves a new reminder.
     * Verifies that the parent application exists first to maintain referential integrity.
     *
     * @param applicationId The unique ID of the application this reminder is for.
     * @param type          The category of the reminder (e.g., DEADLINE, INTERVIEW).
     * @param triggerDate   The date when the reminder should alert the user.
     * @return The newly created Reminder object.
     * @throws IllegalArgumentException if the specified application ID does not exist in storage.
     */
    public Reminder addReminder(String applicationId, ReminderType type, LocalDate triggerDate) {
        // Referential Integrity Check
        boolean appExists = this.storage.loadAllApplications().stream()
                .anyMatch(a -> a.getId().equals(applicationId));

        if (!appExists) {
            throw new IllegalArgumentException("Cannot add reminder: Application ID " + applicationId + " not found.");
        }

        Reminder reminder = new Reminder(applicationId, type, triggerDate);
        storage.saveReminder(reminder);
        return reminder;
    }

    /**
     * Retrieves a chronological list of active reminders scheduled within a specified timeframe.
     * Dismissed reminders are automatically filtered out.
     *
     * @param withinDays The maximum number of days into the future to look for reminders.
     * @return A list of upcoming, active reminders sorted by date ascending.
     */
    public List<Reminder> getUpcomingReminders(int withinDays) {
        LocalDate cutoff = LocalDate.now().plusDays(withinDays);
        return this.storage.loadAllReminders().stream()
                .filter(r -> !r.isDismissed())
                .filter(r -> !r.getTriggerDate().isAfter(cutoff))
                .sorted((a, b) -> a.getTriggerDate().compareTo(b.getTriggerDate()))
                .collect(Collectors.toList());
    }

    /**
     * Dismisses a specific reminder, preventing it from appearing in future active queries.
     * If the reminder ID does not exist, the method silently returns without error.
     *
     * @param reminderId The unique ID of the reminder to dismiss.
     */
    public void dismissReminder(String reminderId) {
        this.storage.loadAllReminders().stream()
                .filter(r -> r.getId().equals(reminderId))
                .findFirst()
                .ifPresent(r -> {
                    r.dismiss();
                    this.storage.updateReminder(r);
                });
    }
}