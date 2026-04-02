package storage;

import logic.Application;
import logic.ApplicationStatus;
import logic.Interview;
import logic.Reminder;
import logic.ReminderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileStorage.
 *
 * Each test runs in a fresh isolated temp directory so tests never affect each other
 * or leave files behind. Edge cases tested include:
 *   - Special characters in user-provided strings (pipe characters)
 *   - Null/empty optional fields (deadline, notes)
 *   - All enum values for ApplicationStatus and ReminderType
 *   - Duplicate saves, updates on missing IDs, deletes on missing IDs
 *   - Data persistence across separate FileStorage instances (same data dir)
 *   - Boundary values for numeric fields (pay = 0, large pay, decimal pay)
 */
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
        // Clean up temp directory after every test
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Creates a minimal Application with APPLIED status and no extras. */
    private Application makeApp(String company, String role) {
        return new Application(company, role, 3000, "Singapore", ApplicationStatus.APPLIED);
    }

    /** Creates a storage instance pointing at the same temp dir (simulates app restart). */
    private FileStorage freshStorage() {
        return new FileStorage(tempDir.toString());
    }

    // =========================================================================
    // Application Tests
    // =========================================================================

    @Nested
    class ApplicationTests {

        @Test
        void loadAll_emptyStorage_returnsEmptyList() {
            // No data has been written — should return empty, not throw
            assertTrue(storage.loadAllApplications().isEmpty());
        }

        @Test
        void save_singleApplication_allFieldsPersisted() {
            Application app = new Application("Google", "SWE Intern", 5000, "Singapore", ApplicationStatus.APPLIED);
            app.setDeadline(LocalDate.of(2025, 6, 1));
            app.setNotes("Referral from a friend");
            storage.saveApplication(app);

            Application loaded = storage.loadAllApplications().get(0);
            assertAll(
                    () -> assertEquals(app.getId(), loaded.getId()),
                    () -> assertEquals("Google", loaded.getCompanyName()),
                    () -> assertEquals("SWE Intern", loaded.getRoleTitle()),
                    () -> assertEquals(5000, loaded.getPay(), 0.001),
                    () -> assertEquals("Singapore", loaded.getLocation()),
                    () -> assertEquals(ApplicationStatus.APPLIED, loaded.getStatus()),
                    () -> assertEquals(app.getDateApplied(), loaded.getDateApplied()),
                    () -> assertEquals(LocalDate.of(2025, 6, 1), loaded.getDeadline()),
                    () -> assertEquals("Referral from a friend", loaded.getNotes())
            );
        }

        @Test
        void save_applicationWithNullDeadline_persistsNullDeadline() {
            // Deadline is optional — null should round-trip cleanly
            Application app = makeApp("Meta", "PM Intern");
            storage.saveApplication(app);

            assertNull(storage.loadAllApplications().get(0).getDeadline());
        }

        @Test
        void save_applicationWithEmptyNotes_persistsEmptyNotes() {
            Application app = makeApp("Grab", "Data Analyst");
            storage.saveApplication(app);

            assertEquals("", storage.loadAllApplications().get(0).getNotes());
        }

        @Test
        void save_applicationWithPipeInFields_escapedAndRestoredCorrectly() {
            // The pipe character '|' is our separator — values containing it must be escaped
            Application app = new Application("Foo|Bar Inc", "Role|Title", 4000, "Loc|ation", ApplicationStatus.APPLIED);
            app.setNotes("Note with | pipe");
            storage.saveApplication(app);

            Application loaded = storage.loadAllApplications().get(0);
            assertEquals("Foo|Bar Inc", loaded.getCompanyName());
            assertEquals("Role|Title", loaded.getRoleTitle());
            assertEquals("Loc|ation", loaded.getLocation());
            assertEquals("Note with | pipe", loaded.getNotes());
        }

        @Test
        void save_duplicateId_ignoredSecondSave() {
            // Saving the same object twice should not create duplicate entries
            Application app = makeApp("Apple", "iOS Intern");
            storage.saveApplication(app);
            storage.saveApplication(app);

            assertEquals(1, storage.loadAllApplications().size());
        }

        @Test
        void save_multipleApplications_allPersistedInOrder() {
            Application a1 = makeApp("CompanyA", "Role1");
            Application a2 = makeApp("CompanyB", "Role2");
            Application a3 = makeApp("CompanyC", "Role3");
            storage.saveApplication(a1);
            storage.saveApplication(a2);
            storage.saveApplication(a3);

            List<Application> all = storage.loadAllApplications();
            assertEquals(3, all.size());
            assertEquals("CompanyA", all.get(0).getCompanyName());
            assertEquals("CompanyB", all.get(1).getCompanyName());
            assertEquals("CompanyC", all.get(2).getCompanyName());
        }

        @Test
        void save_payOfZero_persistedCorrectly() {
            Application app = new Application("Startup", "Unpaid Intern", 0.0, "Remote", ApplicationStatus.APPLIED);
            storage.saveApplication(app);

            assertEquals(0.0, storage.loadAllApplications().get(0).getPay(), 0.001);
        }

        @Test
        void save_payWithDecimals_persistedCorrectly() {
            Application app = new Application("Firm", "Analyst", 3456.78, "NYC", ApplicationStatus.APPLIED);
            storage.saveApplication(app);

            assertEquals(3456.78, storage.loadAllApplications().get(0).getPay(), 0.001);
        }

        @Test
        void save_allApplicationStatusValues_persistedCorrectly() {
            // Every enum variant must survive a save-load cycle
            for (ApplicationStatus status : ApplicationStatus.values()) {
                FileStorage isolatedStorage = new FileStorage(tempDir.resolve("status-" + status.name()).toString());
                Application app = new Application("Co", "Role", 1000, "SG", status);
                isolatedStorage.saveApplication(app);
                assertEquals(status, isolatedStorage.loadAllApplications().get(0).getStatus());
            }
        }

        @Test
        void update_changesOnlyTargetApplication() {
            Application a1 = makeApp("Google", "SWE");
            Application a2 = makeApp("Meta", "PM");
            storage.saveApplication(a1);
            storage.saveApplication(a2);

            a1.setStatus(ApplicationStatus.OFFER);
            a1.setNotes("Got an offer!");
            storage.updateApplication(a1);

            List<Application> all = storage.loadAllApplications();
            Application updatedA1 = all.stream().filter(a -> a.getId().equals(a1.getId())).findFirst().orElseThrow();
            Application unchangedA2 = all.stream().filter(a -> a.getId().equals(a2.getId())).findFirst().orElseThrow();

            assertEquals(ApplicationStatus.OFFER, updatedA1.getStatus());
            assertEquals("Got an offer!", updatedA1.getNotes());
            assertEquals(ApplicationStatus.APPLIED, unchangedA2.getStatus());
        }

        @Test
        void update_nonExistentId_doesNotCorruptExistingData() {
            // Updating an ID that doesn't exist should leave the stored data intact
            Application existing = makeApp("Shopee", "Backend");
            storage.saveApplication(existing);

            Application ghost = new Application("ghost-id", "Nobody", "Nothing", 0, "Nowhere",
                    ApplicationStatus.REJECTED, LocalDate.now(), null, "");
            storage.updateApplication(ghost);

            List<Application> all = storage.loadAllApplications();
            // The ghost entry was added (since update does a replace-or-keep), but existing is safe
            assertTrue(all.stream().anyMatch(a -> a.getId().equals(existing.getId())));
        }

        @Test
        void delete_removesOnlyTargetApplication() {
            Application a1 = makeApp("ToDelete", "Role");
            Application a2 = makeApp("ToKeep", "Role");
            storage.saveApplication(a1);
            storage.saveApplication(a2);

            storage.deleteApplication(a1.getId());

            List<Application> remaining = storage.loadAllApplications();
            assertEquals(1, remaining.size());
            assertEquals("ToKeep", remaining.get(0).getCompanyName());
        }

        @Test
        void delete_onlyEntry_leavesEmptyList() {
            Application app = makeApp("Solo", "Solo Role");
            storage.saveApplication(app);
            storage.deleteApplication(app.getId());

            assertTrue(storage.loadAllApplications().isEmpty());
        }

        @Test
        void delete_nonExistentId_doesNotCorruptExistingData() {
            Application app = makeApp("Real", "Role");
            storage.saveApplication(app);

            // Deleting a bogus ID should be a no-op
            storage.deleteApplication("this-id-does-not-exist");

            assertEquals(1, storage.loadAllApplications().size());
        }

        @Test
        void persistsAcrossStorageInstances_sameDataDirectory() {
            // Verifies that data written by one instance is readable by a separate instance
            Application app = makeApp("Persistent Co", "Engineer");
            app.setNotes("persisted across instances");
            storage.saveApplication(app);

            FileStorage reloaded = freshStorage();
            List<Application> all = reloaded.loadAllApplications();
            assertEquals(1, all.size());
            assertEquals("persisted across instances", all.get(0).getNotes());
        }
    }

    // =========================================================================
    // Interview Tests
    // =========================================================================

    @Nested
    class InterviewTests {

        @Test
        void loadAll_emptyStorage_returnsEmptyList() {
            assertTrue(storage.loadAllInterviews().isEmpty());
        }

        @Test
        void save_singleInterview_allFieldsPersisted() {
            Interview i = new Interview("app-123", 1, LocalDateTime.of(2025, 5, 10, 14, 30));
            i.setNotes("Technical round — LeetCode style");
            storage.saveInterview(i);

            Interview loaded = storage.loadAllInterviews().get(0);
            assertAll(
                    () -> assertEquals(i.getId(), loaded.getId()),
                    () -> assertEquals("app-123", loaded.getApplicationId()),
                    () -> assertEquals(1, loaded.getRound()),
                    () -> assertEquals(LocalDateTime.of(2025, 5, 10, 14, 30), loaded.getDate()),
                    () -> assertEquals("Technical round — LeetCode style", loaded.getNotes())
            );
        }

        @Test
        void save_interviewWithEmptyNotes_persistsEmptyNotes() {
            Interview i = new Interview("app-1", 2, LocalDateTime.now());
            storage.saveInterview(i);

            assertEquals("", storage.loadAllInterviews().get(0).getNotes());
        }

        @Test
        void save_interviewWithPipeInNotes_escapedAndRestoredCorrectly() {
            Interview i = new Interview("app-1", 1, LocalDateTime.of(2025, 1, 1, 9, 0));
            i.setNotes("Interviewer: Alice | Duration: 45 mins");
            storage.saveInterview(i);

            assertEquals("Interviewer: Alice | Duration: 45 mins",
                    storage.loadAllInterviews().get(0).getNotes());
        }

        @Test
        void save_duplicateId_ignoredSecondSave() {
            Interview i = new Interview("app-1", 1, LocalDateTime.now());
            storage.saveInterview(i);
            storage.saveInterview(i);

            assertEquals(1, storage.loadAllInterviews().size());
        }

        @Test
        void save_multipleInterviewsForSameApplication() {
            Interview round1 = new Interview("app-1", 1, LocalDateTime.of(2025, 3, 1, 10, 0));
            Interview round2 = new Interview("app-1", 2, LocalDateTime.of(2025, 3, 8, 10, 0));
            Interview round3 = new Interview("app-1", 3, LocalDateTime.of(2025, 3, 15, 10, 0));
            storage.saveInterview(round1);
            storage.saveInterview(round2);
            storage.saveInterview(round3);

            List<Interview> all = storage.loadAllInterviews();
            assertEquals(3, all.size());
            assertTrue(all.stream().allMatch(i -> i.getApplicationId().equals("app-1")));
        }

        @Test
        void save_interviewsForDifferentApplications_allPersisted() {
            Interview i1 = new Interview("app-A", 1, LocalDateTime.now());
            Interview i2 = new Interview("app-B", 1, LocalDateTime.now());
            storage.saveInterview(i1);
            storage.saveInterview(i2);

            List<Interview> all = storage.loadAllInterviews();
            assertEquals(2, all.size());
        }

        @Test
        void update_changesNotesAndDate() {
            Interview i = new Interview("app-1", 1, LocalDateTime.of(2025, 4, 1, 10, 0));
            storage.saveInterview(i);

            i.setNotes("Updated notes after debrief");
            i.setDate(LocalDateTime.of(2025, 4, 2, 11, 0));
            storage.updateInterview(i);

            Interview loaded = storage.loadAllInterviews().get(0);
            assertEquals("Updated notes after debrief", loaded.getNotes());
            assertEquals(LocalDateTime.of(2025, 4, 2, 11, 0), loaded.getDate());
        }

        @Test
        void update_onlyTargetInterviewChanged() {
            Interview i1 = new Interview("app-1", 1, LocalDateTime.of(2025, 1, 1, 9, 0));
            Interview i2 = new Interview("app-2", 1, LocalDateTime.of(2025, 2, 1, 9, 0));
            storage.saveInterview(i1);
            storage.saveInterview(i2);

            i1.setNotes("Only i1 updated");
            storage.updateInterview(i1);

            Interview reloadedI2 = storage.loadAllInterviews().stream()
                    .filter(i -> i.getId().equals(i2.getId()))
                    .findFirst().orElseThrow();
            assertEquals("", reloadedI2.getNotes());
        }

        @Test
        void persistsAcrossStorageInstances() {
            Interview i = new Interview("app-abc", 2, LocalDateTime.of(2025, 6, 15, 15, 0));
            i.setNotes("Should survive restart");
            storage.saveInterview(i);

            Interview loaded = freshStorage().loadAllInterviews().get(0);
            assertEquals("Should survive restart", loaded.getNotes());
        }
    }

    // =========================================================================
    // Reminder Tests
    // =========================================================================

    @Nested
    class ReminderTests {

        @Test
        void loadAll_emptyStorage_returnsEmptyList() {
            assertTrue(storage.loadAllReminders().isEmpty());
        }

        @Test
        void save_singleReminder_allFieldsPersisted() {
            Reminder r = new Reminder("app-1", ReminderType.DEADLINE, LocalDate.of(2025, 8, 31));
            storage.saveReminder(r);

            Reminder loaded = storage.loadAllReminders().get(0);
            assertAll(
                    () -> assertEquals(r.getId(), loaded.getId()),
                    () -> assertEquals("app-1", loaded.getApplicationId()),
                    () -> assertEquals(ReminderType.DEADLINE, loaded.getType()),
                    () -> assertEquals(LocalDate.of(2025, 8, 31), loaded.getTriggerDate()),
                    () -> assertFalse(loaded.isDismissed())
            );
        }

        @Test
        void save_allReminderTypeValues_persistedCorrectly() {
            // Every ReminderType enum variant must survive a save-load cycle
            for (ReminderType type : ReminderType.values()) {
                FileStorage isolatedStorage = new FileStorage(tempDir.resolve("type-" + type.name()).toString());
                Reminder r = new Reminder("app-1", type, LocalDate.now());
                isolatedStorage.saveReminder(r);
                assertEquals(type, isolatedStorage.loadAllReminders().get(0).getType());
            }
        }

        @Test
        void save_dismissedReminder_persistsDismissedTrue() {
            // Dismissed state must survive a save-load cycle
            Reminder r = new Reminder("app-1", ReminderType.FOLLOWUP, LocalDate.now());
            r.dismiss();
            assertTrue(r.isDismissed(), "Reminder should be dismissed before saving");
            storage.saveReminder(r);

            assertTrue(storage.loadAllReminders().get(0).isDismissed());
        }

        @Test
        void save_duplicateId_ignoredSecondSave() {
            Reminder r = new Reminder("app-1", ReminderType.INTERVIEW, LocalDate.now());
            storage.saveReminder(r);
            storage.saveReminder(r);

            assertEquals(1, storage.loadAllReminders().size());
        }

        @Test
        void save_multipleReminders_allPersisted() {
            Reminder r1 = new Reminder("app-1", ReminderType.DEADLINE, LocalDate.of(2025, 5, 1));
            Reminder r2 = new Reminder("app-2", ReminderType.INTERVIEW, LocalDate.of(2025, 5, 5));
            Reminder r3 = new Reminder("app-3", ReminderType.FOLLOWUP, LocalDate.of(2025, 5, 10));
            storage.saveReminder(r1);
            storage.saveReminder(r2);
            storage.saveReminder(r3);

            assertEquals(3, storage.loadAllReminders().size());
        }

        @Test
        void update_dismissedStatePersistedAfterUpdate() {
            Reminder r = new Reminder("app-1", ReminderType.DEADLINE, LocalDate.of(2025, 6, 1));
            storage.saveReminder(r);
            assertFalse(storage.loadAllReminders().get(0).isDismissed());

            r.dismiss();
            storage.updateReminder(r);

            assertTrue(storage.loadAllReminders().get(0).isDismissed());
        }

        @Test
        void update_onlyTargetReminderChanged() {
            Reminder r1 = new Reminder("app-1", ReminderType.DEADLINE, LocalDate.of(2025, 5, 1));
            Reminder r2 = new Reminder("app-2", ReminderType.INTERVIEW, LocalDate.of(2025, 5, 10));
            storage.saveReminder(r1);
            storage.saveReminder(r2);

            // Dismiss r1 only
            r1.dismiss();
            storage.updateReminder(r1);

            Reminder reloadedR2 = storage.loadAllReminders().stream()
                    .filter(r -> r.getId().equals(r2.getId()))
                    .findFirst().orElseThrow();
            assertFalse(reloadedR2.isDismissed(), "r2 should not be affected by update to r1");
        }

        @Test
        void persistsAcrossStorageInstances() {
            Reminder r = new Reminder("app-xyz", ReminderType.FOLLOWUP, LocalDate.of(2025, 12, 1));
            storage.saveReminder(r);

            Reminder loaded = freshStorage().loadAllReminders().get(0);
            assertEquals(ReminderType.FOLLOWUP, loaded.getType());
            assertEquals(LocalDate.of(2025, 12, 1), loaded.getTriggerDate());
        }
    }

    // =========================================================================
    // Cross-Entity / Storage Infrastructure Tests
    // =========================================================================

    @Nested
    class InfrastructureTests {

        @Test
        void dataDirectoryCreatedAutomatically_whenItDoesNotExist() throws IOException {
            // Use a subdirectory that doesn't exist yet — FileStorage should create it
            Path newDir = tempDir.resolve("subdir/nested");
            FileStorage s = new FileStorage(newDir.toString());
            s.saveApplication(makeApp("Test", "Role"));

            assertTrue(Files.exists(newDir), "Data directory should be created automatically");
        }

        @Test
        void entitiesDoNotInterfereWithEachOther() {
            // Writing applications should not affect interviews or reminders and vice versa
            storage.saveApplication(makeApp("Google", "SWE"));
            storage.saveInterview(new Interview("app-1", 1, LocalDateTime.now()));
            storage.saveReminder(new Reminder("app-1", ReminderType.DEADLINE, LocalDate.now()));

            assertEquals(1, storage.loadAllApplications().size());
            assertEquals(1, storage.loadAllInterviews().size());
            assertEquals(1, storage.loadAllReminders().size());
        }

        @Test
        void deleteAll_multipleApplications_allCanBeRemovedOneByOne() {
            Application a1 = makeApp("A", "Role");
            Application a2 = makeApp("B", "Role");
            Application a3 = makeApp("C", "Role");
            storage.saveApplication(a1);
            storage.saveApplication(a2);
            storage.saveApplication(a3);

            storage.deleteApplication(a1.getId());
            assertEquals(2, storage.loadAllApplications().size());

            storage.deleteApplication(a2.getId());
            assertEquals(1, storage.loadAllApplications().size());

            storage.deleteApplication(a3.getId());
            assertTrue(storage.loadAllApplications().isEmpty());
        }

        @Test
        void largeNumberOfApplications_allSavedAndLoaded() {
            // Stress-test to ensure no silent truncation at scale
            int count = 100;
            for (int i = 0; i < count; i++) {
                storage.saveApplication(makeApp("Company" + i, "Role" + i));
            }
            assertEquals(count, storage.loadAllApplications().size());
        }
    }
}
