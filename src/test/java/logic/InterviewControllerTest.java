package logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InterviewController.
 * Verifies business logic, referential integrity, and note updating functionalities.
 */
class InterviewControllerTest {

    private InterviewController controller;
    private ApplicationController appController;

    /**
     * Initializes the controllers with an in-memory storage stub before each test.
     * Ensures each test runs in complete isolation.
     */
    @BeforeEach
    void setUp() {
        InMemoryStorage storage = new InMemoryStorage();
        controller = new InterviewController(storage);
        appController = new ApplicationController(storage);
    }

    /**
     * Verifies that adding an interview to a valid, existing application successfully
     * creates and stores the interview record.
     */
    @Test
    void addInterview_validInput_returnsInterview() {
        Application app = appController.addApplication(
                "Google", "SWE Intern", 5000, "SG", ApplicationStatus.INTERVIEWING);
        Interview interview = controller.addInterview(
                app.getId(), 1, LocalDateTime.now().plusDays(3));
        assertNotNull(interview);
        assertEquals(1, interview.getRound());
    }

    /**
     * Verifies that when multiple interviews exist for a single application,
     * they are retrieved in ascending order based on their round numbers.
     */
    @Test
    void getInterviewsByApplication_sortedByRound() {
        Application app = appController.addApplication(
                "Meta", "PM Intern", 4000, "SG", ApplicationStatus.INTERVIEWING);
        controller.addInterview(app.getId(), 2, LocalDateTime.now().plusDays(5));
        controller.addInterview(app.getId(), 1, LocalDateTime.now().plusDays(2));

        List<Interview> interviews = controller.getInterviewsByApplication(app.getId());

        assertEquals(1, interviews.get(0).getRound());
        assertEquals(2, interviews.get(1).getRound());
    }

    /**
     * Verifies that updating the notes for an existing interview record
     * successfully saves the changes to storage.
     */
    @Test
    void updateNotes_validId_updatesNotes() {
        Application app = appController.addApplication(
                "Grab", "Data Intern", 3500, "SG", ApplicationStatus.INTERVIEWING);
        Interview interview = controller.addInterview(
                app.getId(), 1, LocalDateTime.now().plusDays(1));

        Interview updated = controller.updateNotes(interview.getId(), "Very friendly interviewer");

        assertEquals("Very friendly interviewer", updated.getNotes());
    }

    /**
     * Verifies that attempting to update notes for an interview ID that does not exist
     * throws an IllegalArgumentException.
     */
    @Test
    void addInterview_invalidAppId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.addInterview("fake-id", 1, LocalDateTime.now().plusDays(1));
        });
    }

    @Test
    void updateNotes_invalidId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.updateNotes("fake-id", "some notes");
        });
    }
}