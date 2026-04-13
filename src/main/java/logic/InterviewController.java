package logic;

import storage.Storage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class InterviewController {
    private final Storage storage;

    public InterviewController(Storage storage) {
        this.storage = storage;
    }

    public Interview addInterview(String applicationId, int round, LocalDateTime date) {
        boolean appExists = storage.loadAllApplications().stream()
                .anyMatch(a -> a.getId().equals(applicationId));
        if (!appExists) {
            throw new IllegalArgumentException(
                    "Cannot add interview: Application ID " + applicationId + " not found.");
        }

        Interview interview = new Interview(applicationId, round, date);
        storage.saveInterview(interview);
        return interview;
    }

    /**
     * Returns all interviews across all applications.
     *
     * @return List of all stored interviews.
     */
    public List<Interview> getAllInterviews() {
        return storage.loadAllInterviews();
    }

    /**
     * Retrieves all interviews associated with a specific application, sorted by round number.
     *
     * @param applicationId The unique ID of the application.
     * @return A list of interviews for the application, sorted in ascending order by round.
     */
    public List<Interview> getInterviewsByApplication(String applicationId) {
        return storage.loadAllInterviews().stream()
                .filter(i -> i.getApplicationId().equals(applicationId))
                .sorted((a, b) -> Integer.compare(a.getRound(), b.getRound()))
                .collect(Collectors.toList());
    }

    public Interview updateNotes(String interviewId, String notes) {
        Interview interview = storage.loadAllInterviews().stream()
                .filter(i -> i.getId().equals(interviewId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        interview.setNotes(notes);
        storage.updateInterview(interview);
        return interview;
    }
}