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
 * Each test gets its own isolated temp directory so tests never interfere with
 * each other or leave files behind on disk.
 *
 * Coverage areas:
 *   - Happy path: save, load, update, delete for all three entity types
 *   - Optional fields: null deadline, empty notes
 *   - Pipe character escaping: '|' in user data must not break the format
 *   - All enum values: ApplicationStatus, ReminderType
 *   - Duplicate prevention: saving the same ID twice
 *   - Isolation: operations on one entity type must not affect others
 *   - Persistence: data written by one FileStorage instance is readable by another
 *   - Boundary values: zero pay, large decimal pay
 *   - Bulk: 100+ records to catch silent truncation
 *   - Resilience: operations on non-existent IDs must not corrupt data
 */
class FileStorageTest {

    private Path tempDir;
    private FileStorage storage;

    @BeforeEach
    void setUp() throws IOException {
        // A fresh isolated directory for every test
        tempDir = Files.createTempDirectory("jobapps-test-");
        storage = new FileStorage(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Walk and delete depth-first so directories are empty before removal
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Minimal Application with no deadline or notes set. */
    private Application makeApp(String company, String role) {
        return new Application(company, role, 3000, "Singapore", ApplicationStatus.APPLIED);
    }

    /**
     * Creates a second FileStorage pointing at the same temp directory.
     * Used to verify that data survives across separate instances (simulates app restart).
     */
    private FileStorage freshStorage() {
        return new FileStorage(tempDir.toString());
    }

    // =========================================================================
    // Application Tests
    // =========================================================================

    /**
     * Tests for Application CRUD operations.
     *
     * Covers saving and loading all fields (including optional deadline and notes),
     * special-character escaping, duplicate prevention, update/delete correctness,
     * boundary values for numeric fields, all ApplicationStatus enum values,
     * and persistence across separate FileStorage instances.
     */
    @Nested
    class ApplicationTests {

        @Test
        void loadAll_noDataFile_returnsEmptyList() {
            // File hasn't been created yet — must return empty, not throw
            assertTrue(storage.loadAllApplications().isEmpty());
        }

        @Test
        void save_allFieldsRoundTrip() {
            // Every field, including optional deadline and notes, must survive write-read
            Application app = new Application("Google", "SWE Intern", 5500.50,
                    "Singapore", ApplicationStatus.APPLIED);
            app.setDeadline(LocalDate.of(2025, 8, 31));
            app.setNotes("Referred by a friend");
            storage.saveApplication(app);

            Application loaded = storage.loadAllApplications().get(0);
            assertAll(
                    () -> assertEquals(app.getId(),           loaded.getId()),
                    () -> assertEquals("Google",              loaded.getCompanyName()),
                    () -> assertEquals("SWE Intern",          loaded.getRoleTitle()),
                    () -> assertEquals(5500.50,               loaded.getPay(), 0.001),
                    () -> assertEquals("Singapore",           loaded.getLocation()),
                    () -> assertEquals(ApplicationStatus.APPLIED, loaded.getStatus()),
                    () -> assertEquals(app.getDateApplied(),  loaded.getDateApplied()),
                    () -> assertEquals(LocalDate.of(2025, 8, 31), loaded.getDeadline()),
                    () -> assertEquals("Referred by a friend", loaded.getNotes())
            );
        }

        @Test
        void save_nullDeadline_loadsAsNull() {
            // Deadline is optional — a blank entry in the file must round-trip to null
            storage.saveApplication(makeApp("Meta", "PM"));
            assertNull(storage.loadAllApplications().get(0).getDeadline());
        }

        @Test
        void save_emptyNotes_loadsAsEmptyString() {
            storage.saveApplication(makeApp("Grab", "Analyst"));
            assertEquals("", storage.loadAllApplications().get(0).getNotes());
        }

        @Test
        void save_pipeCharInUserFields_escapedAndRestoredCorrectly() {
            // '|' is the field separator — data containing it must be escaped so parsing stays correct
            Application app = new Application(
                    "Foo|Bar Corp", "Role|With|Pipes", 4000, "City|Town", ApplicationStatus.APPLIED);
            app.setNotes("Note with | a pipe");
            storage.saveApplication(app);

            Application loaded = storage.loadAllApplications().get(0);
            assertEquals("Foo|Bar Corp",       loaded.getCompanyName());
            assertEquals("Role|With|Pipes",    loaded.getRoleTitle());
            assertEquals("City|Town",          loaded.getLocation());
            assertEquals("Note with | a pipe", loaded.getNotes());
        }

        @Test
        void save_duplicateId_notStoredTwice() {
            // Calling save with the same object twice must not create a duplicate row
            Application app = makeApp("Apple", "iOS Intern");
            storage.saveApplication(app);
            storage.saveApplication(app);
            assertEquals(1, storage.loadAllApplications().size());
        }

        @Test
        void save_multipleEntries_allPersistedInInsertionOrder() {
            Application a1 = makeApp("Alpha", "Intern");
            Application a2 = makeApp("Beta",  "Intern");
            Application a3 = makeApp("Gamma", "Intern");
            storage.saveApplication(a1);
            storage.saveApplication(a2);
            storage.saveApplication(a3);

            List<Application> all = storage.loadAllApplications();
            assertEquals(3,       all.size());
            assertEquals("Alpha", all.get(0).getCompanyName());
            assertEquals("Beta",  all.get(1).getCompanyName());
            assertEquals("Gamma", all.get(2).getCompanyName());
        }

        @Test
        void save_zeroPay_roundTripsCorrectly() {
            Application app = new Application("Nonprofit", "Volunteer", 0.0, "Remote", ApplicationStatus.APPLIED);
            storage.saveApplication(app);
            assertEquals(0.0, storage.loadAllApplications().get(0).getPay(), 0.001);
        }

        @Test
        void save_decimalPay_roundTripsCorrectly() {
            Application app = new Application("Bank", "Analyst", 4567.89, "NYC", ApplicationStatus.APPLIED);
            storage.saveApplication(app);
            assertEquals(4567.89, storage.loadAllApplications().get(0).getPay(), 0.001);
        }

        @Test
        void save_everyApplicationStatus_roundTripsCorrectly() {
            // Ensure every enum variant serialises and deserialises without error
            for (ApplicationStatus status : ApplicationStatus.values()) {
                FileStorage iso = new FileStorage(tempDir.resolve("status-" + status.name()).toString());
                Application app = new Application("Co", "Role", 1000, "SG", status);
                iso.saveApplication(app);
                assertEquals(status, iso.loadAllApplications().get(0).getStatus(),
                        "Failed round-trip for status: " + status);
            }
        }

        @Test
        void update_modifiesOnlyMatchingEntry() {
            Application a1 = makeApp("Google", "SWE");
            Application a2 = makeApp("Meta",   "PM");
            storage.saveApplication(a1);
            storage.saveApplication(a2);

            a1.setStatus(ApplicationStatus.OFFER);
            a1.setNotes("Offer received");
            storage.updateApplication(a1);

            List<Application> all = storage.loadAllApplications();
            Application reloaded1 = all.stream().filter(a -> a.getId().equals(a1.getId())).findFirst().orElseThrow();
            Application reloaded2 = all.stream().filter(a -> a.getId().equals(a2.getId())).findFirst().orElseThrow();

            assertEquals(ApplicationStatus.OFFER, reloaded1.getStatus());
            assertEquals("Offer received",        reloaded1.getNotes());
            // a2 must be completely untouched
            assertEquals(ApplicationStatus.APPLIED, reloaded2.getStatus());
            assertEquals("",                        reloaded2.getNotes());
        }

        @Test
        void update_nonExistentId_doesNotCorruptExistingData() {
            // Update should be a safe no-op for IDs that were never saved
            Application real  = makeApp("Real Co", "Real Role");
            Application ghost = new Application("ghost-id", "Nobody", "Nothing",
                    0, "Nowhere", ApplicationStatus.REJECTED,
                    LocalDate.now(), null, "");
            storage.saveApplication(real);
            storage.updateApplication(ghost);

            // The real entry must still be intact
            assertTrue(storage.loadAllApplications().stream()
                    .anyMatch(a -> a.getId().equals(real.getId())));
        }

        @Test
        void delete_removesOnlyMatchingEntry() {
            Application keep   = makeApp("Keep Me",   "Role");
            Application remove = makeApp("Remove Me", "Role");
            storage.saveApplication(keep);
            storage.saveApplication(remove);

            storage.deleteApplication(remove.getId());

            List<Application> remaining = storage.loadAllApplications();
            assertEquals(1,         remaining.size());
            assertEquals("Keep Me", remaining.get(0).getCompanyName());
        }

        @Test
        void delete_lastEntry_leavesEmptyList() {
            Application app = makeApp("Solo", "Role");
            storage.saveApplication(app);
            storage.deleteApplication(app.getId());
            assertTrue(storage.loadAllApplications().isEmpty());
        }

        @Test
        void delete_nonExistentId_leavesDataIntact() {
            storage.saveApplication(makeApp("Intact", "Role"));
            storage.deleteApplication("this-id-does-not-exist");
            assertEquals(1, storage.loadAllApplications().size());
        }

        @Test
        void persistence_dataReadableByNewStorageInstance() {
            // Verifies that data isn't cached in memory — a brand-new instance must read the same data
            Application app = makeApp("Persist Co", "Engineer");
            app.setNotes("persisted note");
            storage.saveApplication(app);

            List<Application> all = freshStorage().loadAllApplications();
            assertEquals(1,               all.size());
            assertEquals("persisted note", all.get(0).getNotes());
        }
    }

    // =========================================================================
    // Interview Tests
    // =========================================================================

    /**
     * Tests for Interview CRUD operations.
     *
     * Covers saving and loading all fields, pipe-character escaping in notes,
     * duplicate prevention, multiple rounds per application, update of notes
     * and date, and persistence across separate FileStorage instances.
     */
    @Nested
    class InterviewTests {

        @Test
        void loadAll_noDataFile_returnsEmptyList() {
            assertTrue(storage.loadAllInterviews().isEmpty());
        }

        @Test
        void save_allFieldsRoundTrip() {
            Interview i = new Interview("app-123", 1, LocalDateTime.of(2025, 6, 15, 10, 30));
            i.setNotes("System design focus");
            storage.saveInterview(i);

            Interview loaded = storage.loadAllInterviews().get(0);
            assertAll(
                    () -> assertEquals(i.getId(),                                 loaded.getId()),
                    () -> assertEquals("app-123",                                 loaded.getApplicationId()),
                    () -> assertEquals(1,                                          loaded.getRound()),
                    () -> assertEquals(LocalDateTime.of(2025, 6, 15, 10, 30),    loaded.getDate()),
                    () -> assertEquals("System design focus",                     loaded.getNotes())
            );
        }

        @Test
        void save_emptyNotes_loadsAsEmptyString() {
            Interview i = new Interview("app-1", 2, LocalDateTime.now());
            storage.saveInterview(i);
            assertEquals("", storage.loadAllInterviews().get(0).getNotes());
        }

        @Test
        void save_pipeCharInNotes_escapedAndRestoredCorrectly() {
            Interview i = new Interview("app-1", 1, LocalDateTime.of(2025, 1, 1, 9, 0));
            i.setNotes("Interviewer: Alice | Duration: 45 min");
            storage.saveInterview(i);
            assertEquals("Interviewer: Alice | Duration: 45 min",
                    storage.loadAllInterviews().get(0).getNotes());
        }

        @Test
        void save_duplicateId_notStoredTwice() {
            Interview i = new Interview("app-1", 1, LocalDateTime.now());
            storage.saveInterview(i);
            storage.saveInterview(i);
            assertEquals(1, storage.loadAllInterviews().size());
        }

        @Test
        void save_multipleRoundsForSameApplication() {
            Interview r1 = new Interview("app-1", 1, LocalDateTime.of(2025, 3, 1,  10, 0));
            Interview r2 = new Interview("app-1", 2, LocalDateTime.of(2025, 3, 8,  10, 0));
            Interview r3 = new Interview("app-1", 3, LocalDateTime.of(2025, 3, 15, 10, 0));
            storage.saveInterview(r1);
            storage.saveInterview(r2);
            storage.saveInterview(r3);

            List<Interview> all = storage.loadAllInterviews();
            assertEquals(3, all.size());
            assertTrue(all.stream().allMatch(i -> i.getApplicationId().equals("app-1")));
        }

        @Test
        void save_interviewsForDifferentApplications_allPersisted() {
            storage.saveInterview(new Interview("app-A", 1, LocalDateTime.now()));
            storage.saveInterview(new Interview("app-B", 1, LocalDateTime.now()));
            assertEquals(2, storage.loadAllInterviews().size());
        }

        @Test
        void update_modifiesNotesAndDate() {
            Interview i = new Interview("app-1", 1, LocalDateTime.of(2025, 4, 1, 10, 0));
            storage.saveInterview(i);

            i.setNotes("Updated after debrief");
            i.setDate(LocalDateTime.of(2025, 4, 2, 11, 0));
            storage.updateInterview(i);

            Interview loaded = storage.loadAllInterviews().get(0);
            assertEquals("Updated after debrief",                  loaded.getNotes());
            assertEquals(LocalDateTime.of(2025, 4, 2, 11, 0), loaded.getDate());
        }

        @Test
        void update_onlyTargetEntryChanged() {
            Interview i1 = new Interview("app-1", 1, LocalDateTime.of(2025, 1, 1, 9, 0));
            Interview i2 = new Interview("app-2", 1, LocalDateTime.of(2025, 2, 1, 9, 0));
            storage.saveInterview(i1);
            storage.saveInterview(i2);

            i1.setNotes("Only i1 changed");
            storage.updateInterview(i1);

            Interview reloaded2 = storage.loadAllInterviews().stream()
                    .filter(i -> i.getId().equals(i2.getId()))
                    .findFirst().orElseThrow();
            assertEquals("", reloaded2.getNotes());
        }

        @Test
        void persistence_dataReadableByNewStorageInstance() {
            Interview i = new Interview("app-abc", 2, LocalDateTime.of(2025, 7, 1, 14, 0));
            i.setNotes("Survives restart");
            storage.saveInterview(i);

            Interview loaded = freshStorage().loadAllInterviews().get(0);
            assertEquals("Survives restart", loaded.getNotes());
        }
    }

    // =========================================================================
    // Reminder Tests
    // =========================================================================

    /**
     * Tests for Reminder CRUD operations.
     *
     * Covers saving and loading all fields, all ReminderType enum values,
     * persistence of the dismissed flag (both true and false), duplicate
     * prevention, update correctness, and persistence across separate
     * FileStorage instances.
     */
    @Nested
    class ReminderTests {

        @Test
        void loadAll_noDataFile_returnsEmptyList() {
            assertTrue(storage.loadAllReminders().isEmpty());
        }

        @Test
        void save_allFieldsRoundTrip() {
            Reminder r = new Reminder("app-1", ReminderType.DEADLINE, LocalDate.of(2025, 9, 30));
            storage.saveReminder(r);

            Reminder loaded = storage.loadAllReminders().get(0);
            assertAll(
                    () -> assertEquals(r.getId(),                       loaded.getId()),
                    () -> assertEquals("app-1",                         loaded.getApplicationId()),
                    () -> assertEquals(ReminderType.DEADLINE,           loaded.getType()),
                    () -> assertEquals(LocalDate.of(2025, 9, 30),       loaded.getTriggerDate()),
                    () -> assertFalse(loaded.isDismissed())
            );
        }

        @Test
        void save_everyReminderType_roundTripsCorrectly() {
            for (ReminderType type : ReminderType.values()) {
                FileStorage iso = new FileStorage(tempDir.resolve("type-" + type.name()).toString());
                iso.saveReminder(new Reminder("app-1", type, LocalDate.now()));
                assertEquals(type, iso.loadAllReminders().get(0).getType(),
                        "Failed round-trip for type: " + type);
            }
        }

        @Test
        void save_dismissedReminder_persistsDismissedTrue() {
            // A dismissed reminder must still be dismissed after a write-read cycle
            Reminder r = new Reminder("app-1", ReminderType.FOLLOWUP, LocalDate.now());
            r.dismiss();
            assertTrue(r.isDismissed());
            storage.saveReminder(r);

            assertTrue(storage.loadAllReminders().get(0).isDismissed());
        }

        @Test
        void save_duplicateId_notStoredTwice() {
            Reminder r = new Reminder("app-1", ReminderType.INTERVIEW, LocalDate.now());
            storage.saveReminder(r);
            storage.saveReminder(r);
            assertEquals(1, storage.loadAllReminders().size());
        }

        @Test
        void save_multipleReminders_allPersisted() {
            storage.saveReminder(new Reminder("app-1", ReminderType.DEADLINE,  LocalDate.of(2025, 5, 1)));
            storage.saveReminder(new Reminder("app-2", ReminderType.INTERVIEW, LocalDate.of(2025, 5, 5)));
            storage.saveReminder(new Reminder("app-3", ReminderType.FOLLOWUP,  LocalDate.of(2025, 5, 10)));
            assertEquals(3, storage.loadAllReminders().size());
        }

        @Test
        void update_dismissedStatePersistsAfterUpdate() {
            Reminder r = new Reminder("app-1", ReminderType.DEADLINE, LocalDate.of(2025, 6, 1));
            storage.saveReminder(r);
            assertFalse(storage.loadAllReminders().get(0).isDismissed());

            r.dismiss();
            storage.updateReminder(r);
            assertTrue(storage.loadAllReminders().get(0).isDismissed());
        }

        @Test
        void update_onlyTargetEntryChanged() {
            Reminder r1 = new Reminder("app-1", ReminderType.DEADLINE,  LocalDate.of(2025, 5, 1));
            Reminder r2 = new Reminder("app-2", ReminderType.INTERVIEW, LocalDate.of(2025, 5, 10));
            storage.saveReminder(r1);
            storage.saveReminder(r2);

            r1.dismiss();
            storage.updateReminder(r1);

            Reminder reloaded2 = storage.loadAllReminders().stream()
                    .filter(r -> r.getId().equals(r2.getId()))
                    .findFirst().orElseThrow();
            assertFalse(reloaded2.isDismissed(), "r2 should not be affected by update to r1");
        }

        @Test
        void persistence_dataReadableByNewStorageInstance() {
            Reminder r = new Reminder("app-xyz", ReminderType.FOLLOWUP, LocalDate.of(2025, 12, 25));
            storage.saveReminder(r);

            Reminder loaded = freshStorage().loadAllReminders().get(0);
            assertEquals(ReminderType.FOLLOWUP,           loaded.getType());
            assertEquals(LocalDate.of(2025, 12, 25), loaded.getTriggerDate());
        }
    }

    // =========================================================================
    // Infrastructure / Cross-Entity Tests
    // =========================================================================

    /**
     * Tests for storage infrastructure and cross-entity behaviour.
     *
     * Covers automatic data directory creation, isolation between the three
     * entity files, sequential deletion, bulk save/load at scale, and
     * corrupt-line recovery (malformed rows in a .dat file must be silently
     * skipped without affecting valid rows).
     */
    @Nested
    class InfrastructureTests {

        @Test
        void dataDirectory_createdAutomatically_whenMissing() throws IOException {
            // FileStorage must create the directory on first write, even if it doesn't exist
            Path newDir = tempDir.resolve("nested/deep/dir");
            FileStorage s = new FileStorage(newDir.toString());
            s.saveApplication(makeApp("Test", "Role"));
            assertTrue(Files.exists(newDir), "Data directory should be auto-created");
        }

        @Test
        void entityTypes_doNotInterfereWithEachOther() {
            // Writing applications must not corrupt the interviews or reminders file
            storage.saveApplication(makeApp("Google", "SWE"));
            storage.saveInterview(new Interview("app-1", 1, LocalDateTime.now()));
            storage.saveReminder(new Reminder("app-1", ReminderType.DEADLINE, LocalDate.now()));

            assertEquals(1, storage.loadAllApplications().size());
            assertEquals(1, storage.loadAllInterviews().size());
            assertEquals(1, storage.loadAllReminders().size());
        }

        @Test
        void delete_allEntriesOneByOne_leavesEmptyList() {
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
        void bulkSave_100Applications_allLoadedCorrectly() {
            // Catches any silent truncation at scale
            for (int i = 0; i < 100; i++) {
                storage.saveApplication(makeApp("Company" + i, "Role" + i));
            }
            assertEquals(100, storage.loadAllApplications().size());
        }

        @Test
        void bulkSave_preservesAllPayValues() {
            // Ensure numeric serialisation is stable for many records
            for (int i = 0; i < 50; i++) {
                storage.saveApplication(
                        new Application("Co" + i, "Role", i * 100.0, "SG", ApplicationStatus.APPLIED));
            }
            List<Application> all = storage.loadAllApplications();
            for (int i = 0; i < 50; i++) {
                final int idx = i;
                Application a = all.stream()
                        .filter(x -> x.getCompanyName().equals("Co" + idx))
                        .findFirst().orElseThrow();
                assertEquals(i * 100.0, a.getPay(), 0.001);
            }
        }

        @Test
        void corruptApplicationLine_skippedAndValidRowsStillLoaded() throws IOException {
            // Write one valid application followed by a malformed line directly to the file.
            // FileStorage must skip the corrupt row and still return the valid one.
            Application good = makeApp("ValidCo", "Engineer");
            storage.saveApplication(good);

            Path dataFile = tempDir.resolve("applications.dat");
            String corrupt = "this-is-not|enough-fields";  // only 2 fields, needs 9
            Files.writeString(dataFile, Files.readString(dataFile) + corrupt + System.lineSeparator());

            List<Application> loaded = freshStorage().loadAllApplications();
            assertEquals(1, loaded.size(), "Corrupt line should be silently skipped");
            assertEquals("ValidCo", loaded.get(0).getCompanyName());
        }

        @Test
        void corruptInterviewLine_skippedAndValidRowsStillLoaded() throws IOException {
            // Same scenario as above, but for the interviews file.
            Interview good = new Interview("app-1", 1, LocalDateTime.of(2025, 4, 1, 10, 0));
            storage.saveInterview(good);

            Path dataFile = tempDir.resolve("interviews.dat");
            String corrupt = "bad|data";  // only 2 fields, needs 5
            Files.writeString(dataFile, Files.readString(dataFile) + corrupt + System.lineSeparator());

            List<Interview> loaded = freshStorage().loadAllInterviews();
            assertEquals(1, loaded.size(), "Corrupt line should be silently skipped");
            assertEquals("app-1", loaded.get(0).getApplicationId());
        }

        @Test
        void corruptReminderLine_skippedAndValidRowsStillLoaded() throws IOException {
            // Same scenario as above, but for the reminders file.
            Reminder good = new Reminder("app-1", ReminderType.DEADLINE, LocalDate.of(2025, 6, 1));
            storage.saveReminder(good);

            Path dataFile = tempDir.resolve("reminders.dat");
            // Five fields but invalid date and unknown type — parse will fail
            String corrupt = "some-id|app-1|NOT_A_TYPE|not-a-date|false";
            Files.writeString(dataFile, Files.readString(dataFile) + corrupt + System.lineSeparator());

            List<Reminder> loaded = freshStorage().loadAllReminders();
            assertEquals(1, loaded.size(), "Corrupt line should be silently skipped");
            assertEquals(ReminderType.DEADLINE, loaded.get(0).getType());
        }
    }
}
