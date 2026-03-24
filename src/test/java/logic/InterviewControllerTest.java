package logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterviewControllerTest {

    private InterviewController controller;
    private ApplicationController appController;

    @BeforeEach
    void setUp() {
        InMemoryStorage storage = new InMemoryStorage();
        controller = new InterviewController(storage);
        appController = new ApplicationController(storage);
    }

    @Test
    void addInterview_validInput_returnsInterview() {
        Application app = appController.addApplication(
                "Google", "SWE Intern", 5000, "SG", ApplicationStatus.INTERVIEWING);
        Interview interview = controller.addInterview(
                app.getId(), 1, LocalDateTime.now().plusDays(3));
        assertNotNull(interview);
        assertEquals(1, interview.getRound());
    }

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

    @Test
    void updateNotes_validId_updatesNotes() {
        Application app = appController.addApplication(
                "Grab", "Data Intern", 3500, "SG", ApplicationStatus.INTERVIEWING);
        Interview interview = controller.addInterview(
                app.getId(), 1, LocalDateTime.now().plusDays(1));
        Interview updated = controller.updateNotes(interview.getId(), "Very friendly interviewer");
        assertEquals("Very friendly interviewer", updated.getNotes());
    }

    @Test
    void updateNotes_invalidId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.updateNotes("fake-id", "some notes"));
    }
}