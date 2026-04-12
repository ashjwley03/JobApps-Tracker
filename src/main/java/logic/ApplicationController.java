package logic;

import storage.Storage;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the business logic for Job Applications.
 * This controller enforces status transitions, validates input, and coordinates
 * with the storage layer to persist data.
 */
public class ApplicationController {
    private final Storage storage;

    /**
     * Constructs an ApplicationController with a specified storage dependency.
     *
     * @param storage The storage implementation to use for data persistence.
     */
    public ApplicationController(Storage storage) {
        this.storage = storage;
    }

    /**
     * Creates and persists a new job application.
     *
     * @param companyName Name of the hiring company.
     * @param roleTitle   Title of the job position.
     * @param pay         Offered or expected salary.
     * @param location    Job location.
     * @param status      Initial status of the application.
     * @return The newly created Application object.
     * @throws IllegalArgumentException if the company name or role title is null or blank.
     */
    public Application addApplication(String companyName, String roleTitle,
                                      double pay, String location,
                                      ApplicationStatus status) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name cannot be empty.");
        }
        if (roleTitle == null || roleTitle.isBlank()) {
            throw new IllegalArgumentException("Role title cannot be empty.");
        }

        Application app = new Application(companyName, roleTitle, pay, location, status);
        this.storage.saveApplication(app);
        return app;
    }

    /**
     * Retrieves all stored job applications.
     *
     * @return A list of all applications.
     */
    public List<Application> getAllApplications() {
        return this.storage.loadAllApplications();
    }

    /**
     * Finds a specific application by its unique ID.
     *
     * @param id The unique identifier of the application.
     * @return The found Application object.
     * @throws IllegalArgumentException if no application exists with the provided ID.
     */
    public Application getApplicationById(String id) {
        return this.storage.loadAllApplications().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
    }

    /**
     * Updates the status of an existing application while enforcing valid flow rules.
     * Transitions are checked to ensure consistency with the documented status-flow.
     *
     * @param id        The ID of the application to update.
     * @param newStatus The target status.
     * @return The updated Application object.
     * @throws IllegalStateException if the status transition violates business flow rules
     * (e.g., modifying a REJECTED application).
     */
    public Application updateStatus(String id, ApplicationStatus newStatus) {
        Application app = this.getApplicationById(id);
        ApplicationStatus current = app.getStatus();

        // Rule: REJECTED is a terminal state (mismatch fix)
        if (current == ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Cannot change status: Application is already REJECTED.");
        }

        // Rule: ACCEPTED is a terminal state
        if (current == ApplicationStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot change status: Application is already ACCEPTED.");
        }

        // Rule : Withdrawn is a terminal state
        if (current == ApplicationStatus.WITHDRAWN) {
            throw new IllegalStateException("Cannot change status: Application is already WITHDRAWN.");
        }

        // Rule: Must be INTERVIEWING to receive an OFFER
        if (current == ApplicationStatus.APPLIED && newStatus == ApplicationStatus.OFFER) {
            throw new IllegalStateException("Invalid transition: Must interview before receiving an offer.");
        }

        app.setStatus(newStatus);
        this.storage.updateApplication(app);
        return app;
    }

    /**
     * Deletes an application from storage.
     *
     * @param id The unique ID of the application to delete.
     */
    public void deleteApplication(String id) {
        this.getApplicationById(id);
        this.storage.deleteApplication(id);
    }

    /**
     * Compares applications by pay in descending order.
     *
     * @param ids List of IDs for the applications to be compared.
     * @return A sorted list of applications.
     */
    public List<Application> compareApplications(List<String> ids) {
        return ids.stream()
                .map(id -> {
                    return this.getApplicationById(id);
                })
                .sorted((a, b) -> {
                    return Double.compare(b.getPay(), a.getPay());
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters and retrieves all applications matching a specific status.
     *
     * @param status The target ApplicationStatus to filter by.
     * @return A list of applications whose current status matches the given status.
     */
    public List<Application> filterByStatus(ApplicationStatus status) {
        return storage.loadAllApplications().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }
}