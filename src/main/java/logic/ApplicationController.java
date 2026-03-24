package logic;

import storage.Storage;

import java.util.List;
import java.util.stream.Collectors;

public class ApplicationController {
    private final Storage storage;

    public ApplicationController(Storage storage) {
        this.storage = storage;
    }

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
        storage.saveApplication(app);
        return app;
    }

    public List<Application> getAllApplications() {
        return storage.loadAllApplications();
    }

    public Application getApplicationById(String id) {
        return storage.loadAllApplications().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
    }

    public Application updateStatus(String id, ApplicationStatus newStatus) {
        Application app = getApplicationById(id);
        app.setStatus(newStatus);
        storage.updateApplication(app);
        return app;
    }

    public void deleteApplication(String id) {
        getApplicationById(id);
        storage.deleteApplication(id);
    }

    public List<Application> compareApplications(List<String> ids) {
        return ids.stream()
                .map(this::getApplicationById)
                .sorted((a, b) -> Double.compare(b.getPay(), a.getPay()))
                .collect(Collectors.toList());
    }

    public List<Application> filterByStatus(ApplicationStatus status) {
        return storage.loadAllApplications().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }
}