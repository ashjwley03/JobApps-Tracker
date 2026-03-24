package logic;

/**
 * Represents a reminder (offer deadline, interview, follow-up, etc.).
 * Logic layer (Yugam) owns the full definition — this is a minimal stub for Storage to use.
 */
public class Reminder {
    private String id;
    private String description;
    private String dueDate;
    private String type;

    public Reminder(String id, String description, String dueDate, String type) {
        this.id = id;
        this.description = description;
        this.dueDate = dueDate;
        this.type = type;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getDueDate() { return dueDate; }
    public String getType() { return type; }

    public void setId(String id) { this.id = id; }
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setType(String type) { this.type = type; }
}
