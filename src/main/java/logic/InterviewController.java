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
        Interview interview = new Interview(applicationId, round, date);
        storage.saveInterview(interview);
        return interview;
    }

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