package logic;

/**
 * Represents an interview tied to an application.
 * Logic layer (Yugam) owns the full definition — this is a minimal stub for Storage to use.
 */
public class Interview {
    private String id;
    private String applicationId;
    private int round;
    private String dateTime;

    public Interview(String id, String applicationId, int round, String dateTime) {
        this.id = id;
        this.applicationId = applicationId;
        this.round = round;
        this.dateTime = dateTime;
    }

    public String getId() { return id; }
    public String getApplicationId() { return applicationId; }
    public int getRound() { return round; }
    public String getDateTime() { return dateTime; }

    public void setId(String id) { this.id = id; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public void setRound(int round) { this.round = round; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
}
