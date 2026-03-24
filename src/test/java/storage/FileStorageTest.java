package storage;

import logic.Application;
import logic.ApplicationStatus;
import logic.Interview;
import logic.Reminder;
import logic.ReminderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageTest {

    private Path tempDir;
    private FileStorage storage;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("jobapps-test-");
        storage = new FileStorage(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir).sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }
    }

    @Test
    void saveAndLoadApplications_persistsCorrectly() {
        Application app = new Application("Google", "SWE", 5000, "SG", ApplicationStatus.APPLIED);
        storage.saveApplication(app);
        List<Application> loaded = storage.loadAllApplications();
        assertEquals(1, loaded.size());
        assertEquals(app.getId(), loaded.get(0).getId());
        assertEquals("Google", loaded.get(0).getCompanyName());
        assertEquals(ApplicationStatus.APPLIED, loaded.get(0).getStatus());
    }

    @Test
    void updateAndDeleteApplication_works() {
        Application app = new Application("Meta", "PM", 4000, "Remote", ApplicationStatus.APPLIED);
        storage.saveApplication(app);
        app.setStatus(ApplicationStatus.OFFER);
        storage.updateApplication(app);
        assertEquals(ApplicationStatus.OFFER, storage.loadAllApplications().get(0).getStatus());
        storage.deleteApplication(app.getId());
        assertTrue(storage.loadAllApplications().isEmpty());
    }

    @Test
    void saveAndLoadInterviews_persistsCorrectly() {
        Interview i = new Interview("app-1", 1, LocalDateTime.of(2025, 4, 1, 10, 0));
        storage.saveInterview(i);
        List<Interview> loaded = storage.loadAllInterviews();
        assertEquals(1, loaded.size());
        assertEquals(i.getId(), loaded.get(0).getId());
        assertEquals("app-1", loaded.get(0).getApplicationId());
        assertEquals(1, loaded.get(0).getRound());
    }

    @Test
    void saveAndLoadReminders_persistsCorrectly() {
        Reminder r = new Reminder("app-1", ReminderType.DEADLINE, LocalDate.of(2025, 4, 15));
        storage.saveReminder(r);
        List<Reminder> loaded = storage.loadAllReminders();
        assertEquals(1, loaded.size());
        assertEquals(r.getId(), loaded.get(0).getId());
        assertEquals(ReminderType.DEADLINE, loaded.get(0).getType());
        assertFalse(loaded.get(0).isDismissed());
    }
}
