package logic;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a notification or task reminder linked to a job application.
 * Tracks the type of reminder, its trigger date, and whether the user has dismissed it.
 */
public class Reminder {
    private final String id;
    private final String applicationId;
    private final ReminderType type;
    private final LocalDate triggerDate;
    private boolean dismissed;

    /**
     * Standard constructor for creating a new reminder.
     * Automatically generates a unique identifier and sets the initial state to not dismissed.
     *
     * @param applicationId The unique ID of the associated job application.
     * @param type          The category/type of the reminder.
     * @param triggerDate   The date when this reminder should trigger.
     */
    public Reminder(String applicationId, ReminderType type, LocalDate triggerDate) {
        this.id = UUID.randomUUID().toString();
        this.applicationId = applicationId;
        this.type = type;
        this.triggerDate = triggerDate;
        this.dismissed = false;
    }

    /**
     * Full constructor used primarily by the storage layer when loading existing records from disk.
     *
     * @param id            The unique identifier of the reminder.
     * @param applicationId The unique ID of the associated job application.
     * @param type          The category/type of the reminder.
     * @param triggerDate   The date when this reminder should trigger.
     * @param dismissed     The current dismissal state of the reminder.
     */
    public Reminder(String id, String applicationId, ReminderType type,
                    LocalDate triggerDate, boolean dismissed) {
        this.id = id;
        this.applicationId = applicationId;
        this.type = type;
        this.triggerDate = triggerDate;
        this.dismissed = dismissed;
    }

    /**
     * @return The unique identifier of the reminder.
     */
    public String getId() {
        return id;
    }

    /**
     * @return The unique ID of the parent application.
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * @return The type or category of the reminder.
     */
    public ReminderType getType() {
        return type;
    }

    /**
     * @return The date on which the reminder is scheduled to trigger.
     */
    public LocalDate getTriggerDate() {
        return triggerDate;
    }

    /**
     * @return True if the reminder has been dismissed by the user, false otherwise.
     */
    public boolean isDismissed() {
        return dismissed;
    }

    /**
     * Marks this reminder as dismissed, indicating it should no longer trigger or be displayed as active.
     */
    public void dismiss() {
        this.dismissed = true;
    }
}