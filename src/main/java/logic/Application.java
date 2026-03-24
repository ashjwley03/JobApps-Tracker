package logic;

import java.time.LocalDate;
import java.util.UUID;

public class Application {
    private final String id;
    private String companyName;
    private String roleTitle;
    private double pay;
    private String location;
    private ApplicationStatus status;
    private final LocalDate dateApplied;
    private LocalDate deadline;
    private String notes;

    public Application(String companyName, String roleTitle, double pay,
                       String location, ApplicationStatus status) {
        this.id = UUID.randomUUID().toString();
        this.companyName = companyName;
        this.roleTitle = roleTitle;
        this.pay = pay;
        this.location = location;
        this.status = status;
        this.dateApplied = LocalDate.now();
        this.deadline = null;
        this.notes = "";
    }

    /** Constructor for restoring from storage. */
    public Application(String id, String companyName, String roleTitle, double pay,
                       String location, ApplicationStatus status, LocalDate dateApplied,
                       LocalDate deadline, String notes) {
        this.id = id;
        this.companyName = companyName;
        this.roleTitle = roleTitle;
        this.pay = pay;
        this.location = location;
        this.status = status;
        this.dateApplied = dateApplied;
        this.deadline = deadline;
        this.notes = notes != null ? notes : "";
    }

    public String getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getRoleTitle() { return roleTitle; }
    public double getPay() { return pay; }
    public String getLocation() { return location; }
    public ApplicationStatus getStatus() { return status; }
    public LocalDate getDateApplied() { return dateApplied; }
    public LocalDate getDeadline() { return deadline; }
    public String getNotes() { return notes; }

    public void setStatus(ApplicationStatus status) { this.status = status; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return String.format("[%s] %s @ %s | %s | $%.0f | %s",
                id.substring(0, 8), roleTitle, companyName, location, pay, status);
    }
}