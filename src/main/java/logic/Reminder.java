package logic;

import java.time.LocalDate;
import java.util.UUID;

public class Reminder {
    private final String id;
    private final String applicationId;
    private final ReminderType type;
    private final LocalDate triggerDate;
    private boolean dismissed;

    public Reminder(String applicationId, ReminderType type, LocalDate triggerDate) {
        this.id = UUID.randomUUID().toString();
        this.applicationId = applicationId;
        this.type = type;
        this.triggerDate = triggerDate;
        this.dismissed = false;
    }

    /** Constructor for restoring from storage. */
    public Reminder(String id, String applicationId, ReminderType type, LocalDate triggerDate, boolean dismissed) {
        this.id = id;
        this.applicationId = applicationId;
        this.type = type;
        this.triggerDate = triggerDate;
        this.dismissed = dismissed;
    }

    public String getId() { return id; }
    public String getApplicationId() { return applicationId; }
    public ReminderType getType() { return type; }
    public LocalDate getTriggerDate() { return triggerDate; }
    public boolean isDismissed() { return dismissed; }

    public void dismiss() { this.dismissed = true; }
}