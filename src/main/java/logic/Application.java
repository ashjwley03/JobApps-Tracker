package logic;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a job application and its associated details.
 * Tracks company information, role, salary, and current status.
 */
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

    /**
     * Standard constructor for creating a new application.
     * Generates a new UUID and sets the application date to today.
     *
     * @param companyName The name of the company applying to.
     * @param roleTitle The title of the position.
     * @param pay The expected or offered salary.
     * @param location The geographical location of the job.
     * @param status The initial status of the application.
     */
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

    /**
     * Full constructor used for loading existing applications from storage.
     *
     * @param id The unique identifier of the application.
     * @param companyName The name of the company applying to.
     * @param roleTitle The title of the position.
     * @param pay The expected or offered salary.
     * @param location The geographical location of the job.
     * @param status The current status of the application.
     * @param dateApplied The original date the application was submitted.
     * @param deadline The deadline for the application or offer (can be null).
     * @param notes Additional user notes.
     */
    public Application(String id, String companyName, String roleTitle, double pay,
                       String location, ApplicationStatus status,
                       LocalDate dateApplied, LocalDate deadline, String notes) {
        this.id = id;
        this.companyName = companyName;
        this.roleTitle = roleTitle;
        this.pay = pay;
        this.location = location;
        this.status = status;
        this.dateApplied = dateApplied;
        this.deadline = deadline;
        this.notes = notes;
    }

    /**
     * @return The unique identifier of the application.
     */
    public String getId() {
        return id;
    }

    /**
     * @return The name of the company.
     */
    public String getCompanyName() {
        return companyName;
    }

    /**
     * @return The job role title.
     */
    public String getRoleTitle() {
        return roleTitle;
    }

    /**
     * @return The salary or pay associated with the role.
     */
    public double getPay() {
        return pay;
    }

    /**
     * @return The location of the job.
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return The current status of the application.
     */
    public ApplicationStatus getStatus() {
        return status;
    }

    /**
     * @return The date the application was created/applied.
     */
    public LocalDate getDateApplied() {
        return dateApplied;
    }

    /**
     * @return The deadline associated with the application, or null if none.
     */
    public LocalDate getDeadline() {
        return deadline;
    }

    /**
     * @return Any custom notes added to the application.
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Updates the company name.
     * @param companyName The new company name.
     */
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    /**
     * Updates the role title.
     * @param roleTitle The new role title.
     */
    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    /**
     * Updates the pay value.
     * @param pay The new pay amount.
     */
    public void setPay(double pay) {
        this.pay = pay;
    }

    /**
     * Updates the location.
     * @param location The new location.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Updates the current status of the application.
     * @param status The new status to apply.
     */
    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    /**
     * Sets or updates the deadline for the application.
     * @param deadline The new deadline date.
     */
    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    /**
     * Sets or updates the custom notes for the application.
     * @param notes The new notes text.
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returns a formatted string representation of the application details.
     * @return A summary string of the application.
     */
    @Override
    public String toString() {
        return String.format("[%s] %s @ %s | %s | $%.0f | %s",
                id.substring(0, 8), roleTitle, companyName, location, pay, status);
    }
}