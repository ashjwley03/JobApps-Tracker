package logic;

import java.time.LocalDateTime;
import java.util.UUID;

public class Interview {
    private final String id;
    private final String applicationId;
    private final int round;
    private LocalDateTime date;
    private String notes;

    // Standard constructor — used when creating a new interview
    public Interview(String applicationId, int round, LocalDateTime date) {
        this.id = UUID.randomUUID().toString();
        this.applicationId = applicationId;
        this.round = round;
        this.date = date;
        this.notes = "";
    }

    // Full constructor — used by FileStorage when loading from disk
    public Interview(String id, String applicationId, int round,
                     LocalDateTime date, String notes) {
        this.id = id;
        this.applicationId = applicationId;
        this.round = round;
        this.date = date;
        this.notes = notes;
    }

    public String getId() { return id; }
    public String getApplicationId() { return applicationId; }
    public int getRound() { return round; }
    public LocalDateTime getDate() { return date; }
    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }
    public void setDate(LocalDateTime date) { this.date = date; }
}