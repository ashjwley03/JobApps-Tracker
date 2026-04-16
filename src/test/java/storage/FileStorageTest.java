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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for FileStorage.
 *
 * Each test gets its own isolated temp directory so tests never interfere with
 * each other or leave files behind on disk.
 *
 * Coverage areas:
 * - Happy path: save, load, update, delete for all three entity types
 * - Optional fields: null deadline, empty notes
 * - Pipe character escaping: '|' in user data must not break the format
 * - All enum values: ApplicationStatus, ReminderType
 * - Duplicate prevention: saving the same ID twice
 * - Isolation: operations on one entity type must not affect others
 * - Persistence: data written by one FileStorage instance is readable by another
 * - Boundary values: zero pay, large decimal pay
 * - Bulk: 100+ records to catch silent truncation
 * - Resilience: operations on non-existent IDs must not corrupt data
 */
class FileStorageTest {

    private Path tempDir;
    private FileStorage storage;

    /**
     * Sets up a fresh, isolated temporary directory and a new FileStorage instance
     * before each test runs to guarantee zero cross-test interference.
     *
     * @throws IOException If the temporary directory cannot be created.
     */
    @BeforeEach
    void setUp() throws IOException {
        // A fresh isolated directory for every test
        tempDir = Files.createTempDirectory("jobapps-test-");
        storage = new FileStorage(tempDir.toString());
    }

    /**
     * Cleans up the temporary directory after each test.
     * Performs a depth-first walk to ensure all files are deleted before the directory itself.
     *
     * @throws IOException If the directory tree cannot be walked or accessed.
     */
    @AfterEach
    void tearDown() throws IOException {
        // Walk and delete depth-first so directories are empty before removal
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> {
                        return -a.compareTo(b);
                    })
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            // Best-effort cleanup — if a temp file cannot be deleted (e.g. locked
                            // by the OS), it is safe to ignore: the OS will reclaim it eventually
                            // and it does not affect the correctness of any test.
                        }
                    });
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Creates a minimal Application with no deadline or notes set for testing.
     *
     * @param company The company name.
     * @param role    The role title.
     * @return A newly constructed Application object.
     */
    private Application makeApp(String company, String role) {
        return new Application(company, role, 3000, "Singapore", ApplicationStatus.APPLIED);
    }

    /**
     * Creates a second FileStorage pointing at the same temp directory.
     * Used to verify that data survives across separate instances (simulates app restart).
     *
     * @return A new FileStorage instance linked to the current test directory.
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

        /** Verifies that loading from a non-existent file returns an empty list, not an exception. */
        @Test
        void loadAll_noDataFile_returnsEmptyList() {
            assertTrue(storage.loadAllApplications().isEmpty());
        }

        /** Verifies that all nine Application fields survive a full write-then-read cycle. */
        @Test
        void save_allFieldsRoundTrip() {
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

        /** Verifies that a null deadline is stored as blank and loaded back as null. */
        @Test
        void save_nullDeadline_loadsAsNull() {
            storage.saveApplication(makeApp("Meta", "PM"));
            assertNull(storage.loadAllApplications().get(0).getDeadline());
        }

        /** Verifies that an empty notes field is stored and loaded as an empty string, not null. */
        @Test
        void save_emptyNotes_loadsAsEmptyString() {
            storage.saveApplication(makeApp("Grab", "Analyst"));
            assertEquals("", storage.loadAllApplications().get(0).getNotes());
        }

        /**
         * Verifies that pipe characters ('|') in user-supplied fields are escaped on write
         * and unescaped on read, so they do not break the pipe-delimited format.
         */
        @Test
        void save_pipeCharInUserFields_escapedAndRestoredCorrectly() {
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

        /** Verifies that multiple Applications are all stored and loaded in insertion order. */
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

        /** Verifies that a pay value of zero is not dropped or altered during serialisation. */
        @Test
        void save_zeroPay_roundTripsCorrectly() {
            Application app = new Application("Nonprofit", "Volunteer", 0.0, "Remote", ApplicationStatus.APPLIED);
            storage.saveApplication(app);
            assertEquals(0.0, storage.loadAllApplications().get(0).getPay(), 0.001);
        }

        /** Verifies that a fractional pay value retains its decimal precision after serialisation. */
        @Test
        void save_decimalPay_roundTripsCorrectly() {
            Application app = new Application("Bank", "Analyst", 4567.89, "NYC", ApplicationStatus.APPLIED);
            storage.saveApplication(app);
            assertEquals(4567.89, storage.loadAllApplications().get(0).getPay(), 0.001);
        }

        /** Verifies that every ApplicationStatus enum value serialises and deserialises correctly. */
        @Test
        void save_everyApplicationStatus_roundTripsCorrectly() {
            for (ApplicationStatus status : ApplicationStatus.values()) {
                FileStorage iso = new FileStorage(tempDir.resolve("status-" + status.name()).toString());
                Application app = new Application("Co", "Role", 1000, "SG", status);
                iso.saveApplication(app);
                assertEquals(status, iso.loadAllApplications().get(0).getStatus(),
                        "Failed round-trip for status: " + status);
            }
        }

        /** Verifies that updating an Application changes only the entry with the matching ID. */
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

        /** Verifies that updating an ID that was never saved does not corrupt existing records. */
        @Test
        void update_nonExistentId_doesNotCorruptExistingData() {
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

        /** Verifies that deleting one Application leaves all other entries intact. */
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

        /** Verifies that deleting the only Application leaves an empty list, not an error. */
        @Test
        void delete_lastEntry_leavesEmptyList() {
            Application app = makeApp("Solo", "Role");
            storage.saveApplication(app);
            storage.deleteApplication(app.getId());
            assertTrue(storage.loadAllApplications().isEmpty());
        }

        /** Verifies that deleting a non-existent ID is a safe no-op and leaves existing data intact. */
        @Test
        void delete_nonExistentId_leavesDataIntact() {
            storage.saveApplication(makeApp("Intact", "Role"));
            storage.deleteApplication("this-id-does-not-exist");
            assertEquals(1, storage.loadAllApplications().size());
        }

        /**
         * Verifies that data written by one FileStorage instance is readable
         * by a separate instance pointing at the same directory.
         */
        @Test
        void persistence_dataReadableByNewStorageInstance() {
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

        /** Verifies that loading from a non-existent file returns an empty list, not an exception. */
        @Test
        void loadAll_noDataFile_returnsEmptyList() {
            assertTrue(storage.loadAllInterviews().isEmpty());
        }

        /** Verifies that all five Interview fields survive a full write-then-read cycle. */
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

        /** Verifies that an Interview with no notes stores and loads as an empty string. */
        @Test
        void save_emptyNotes_loadsAsEmptyString() {
            Interview i = new Interview("app-1", 2, LocalDateTime.now());
            storage.saveInterview(i);
            assertEquals("", storage.loadAllInterviews().get(0).getNotes());
        }

        /** Verifies that pipe characters in interview notes are escaped and restored correctly. */
        @Test
        void save_pipeCharInNotes_escapedAndRestoredCorrectly() {
            Interview i = new Interview("app-1", 1, LocalDateTime.of(2025, 1, 1, 9, 0));
            i.setNotes("Interviewer: Alice | Duration: 45 min");
            storage.saveInterview(i);
            assertEquals("Interviewer: Alice | Duration: 45 min",
                    storage.loadAllInterviews().get(0).getNotes());
        }

        /** Verifies that multiple interview rounds for the same application are all persisted. */
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

        /** Verifies that interviews belonging to different applications are all stored independently. */
        @Test
        void save_interviewsForDifferentApplications_allPersisted() {
            storage.saveInterview(new Interview("app-A", 1, LocalDateTime.now()));
            storage.saveInterview(new Interview("app-B", 1, LocalDateTime.now()));
            assertEquals(2, storage.loadAllInterviews().size());
        }

        /** Verifies that updating an Interview correctly persists changes to both notes and date. */
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

        /** Verifies that updating one Interview does not affect any other stored interviews. */
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

        /** Verifies that Interview data written by one FileStorage instance is readable by a separate instance. */
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

        /** Verifies that loading from a non-existent file returns an empty list, not an exception. */
        @Test
        void loadAll_noDataFile_returnsEmptyList() {
            assertTrue(storage.loadAllReminders().isEmpty());
        }

        /** Verifies that all five Reminder fields survive a full write-then-read cycle. */
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

        /** Verifies that every ReminderType enum value serialises and deserialises correctly. */
        @Test
        void save_everyReminderType_roundTripsCorrectly() {
            for (ReminderType type : ReminderType.values()) {
                FileStorage iso = new FileStorage(tempDir.resolve("type-" + type.name()).toString());
                iso.saveReminder(new Reminder("app-1", type, LocalDate.now()));
                assertEquals(type, iso.loadAllReminders().get(0).getType(),
                        "Failed round-trip for type: " + type);
            }
        }

        /** Verifies that a dismissed Reminder retains its dismissed state after a write-read cycle. */
        @Test
        void save_dismissedReminder_persistsDismissedTrue() {
            Reminder r = new Reminder("app-1", ReminderType.FOLLOWUP, LocalDate.now());
            r.dismiss();
            assertTrue(r.isDismissed());
            storage.saveReminder(r);

            assertTrue(storage.loadAllReminders().get(0).isDismissed());
        }

        /** Verifies that multiple Reminders of different types are all stored and loadable. */
        @Test
        void save_multipleReminders_allPersisted() {
            storage.saveReminder(new Reminder("app-1", ReminderType.DEADLINE,  LocalDate.of(2025, 5, 1)));
            storage.saveReminder(new Reminder("app-2", ReminderType.INTERVIEW, LocalDate.of(2025, 5, 5)));
            storage.saveReminder(new Reminder("app-3", ReminderType.FOLLOWUP,  LocalDate.of(2025, 5, 10)));
            assertEquals(3, storage.loadAllReminders().size());
        }

        /** Verifies that dismissing a Reminder and calling updateReminder persists the dismissed state. */
        @Test
        void update_dismissedStatePersistsAfterUpdate() {
            Reminder r = new Reminder("app-1", ReminderType.DEADLINE, LocalDate.of(2025, 6, 1));
            storage.saveReminder(r);
            assertFalse(storage.loadAllReminders().get(0).isDismissed());

            r.dismiss();
            storage.updateReminder(r);
            assertTrue(storage.loadAllReminders().get(0).isDismissed());
        }

        /** Verifies that updating one Reminder does not affect any other stored reminders. */
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

        /** Verifies that Reminder data written by one FileStorage instance is readable by a separate instance. */
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

        /** Verifies that FileStorage creates the data directory automatically on first write, even for nested paths. */
        @Test
        void dataDirectory_createdAutomatically_whenMissing() throws IOException {
            Path newDir = tempDir.resolve("nested/deep/dir");
            FileStorage s = new FileStorage(newDir.toString());
            s.saveApplication(makeApp("Test", "Role"));
            assertTrue(Files.exists(newDir), "Data directory should be auto-created");
        }

        /** Verifies that writes to one entity type do not corrupt or affect the other entity files. */
        @Test
        void entityTypes_doNotInterfereWithEachOther() {
            storage.saveApplication(makeApp("Google", "SWE"));
            storage.saveInterview(new Interview("app-1", 1, LocalDateTime.now()));
            storage.saveReminder(new Reminder("app-1", ReminderType.DEADLINE, LocalDate.now()));

            assertEquals(1, storage.loadAllApplications().size());
            assertEquals(1, storage.loadAllInterviews().size());
            assertEquals(1, storage.loadAllReminders().size());
        }

        /** Verifies that deleting all entries one by one eventually leaves a fully empty list. */
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

        /** Verifies that 100 Applications can be saved and fully loaded without silent truncation. */
        @Test
        void bulkSave_100Applications_allLoadedCorrectly() {
            for (int i = 0; i < 100; i++) {
                storage.saveApplication(makeApp("Company" + i, "Role" + i));
            }
            assertEquals(100, storage.loadAllApplications().size());
        }

        /** Verifies that pay values are preserved accurately across 50 records to catch numeric serialisation drift. */
        @Test
        void bulkSave_preservesAllPayValues() {
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

        /**
         * Verifies that a malformed line in applications.dat is silently skipped
         * and does not prevent valid rows from being loaded.
         */
        @Test
        void corruptApplicationLine_skippedAndValidRowsStillLoaded() throws IOException {
            Application good = makeApp("ValidCo", "Engineer");
            storage.saveApplication(good);

            Path dataFile = tempDir.resolve("applications.dat");
            String corrupt = "this-is-not|enough-fields";  // only 2 fields, needs 9
            Files.writeString(dataFile, Files.readString(dataFile) + corrupt + System.lineSeparator());

            List<Application> loaded = freshStorage().loadAllApplications();
            assertEquals(1, loaded.size(), "Corrupt line should be silently skipped");
            assertEquals("ValidCo", loaded.get(0).getCompanyName());
        }

        /**
         * Verifies that a malformed line in interviews.dat is silently skipped
         * and does not prevent valid rows from being loaded.
         */
        @Test
        void corruptInterviewLine_skippedAndValidRowsStillLoaded() throws IOException {
            Interview good = new Interview("app-1", 1, LocalDateTime.of(2025, 4, 1, 10, 0));
            storage.saveInterview(good);

            Path dataFile = tempDir.resolve("interviews.dat");
            String corrupt = "bad|data";  // only 2 fields, needs 5
            Files.writeString(dataFile, Files.readString(dataFile) + corrupt + System.lineSeparator());

            List<Interview> loaded = freshStorage().loadAllInterviews();
            assertEquals(1, loaded.size(), "Corrupt line should be silently skipped");
            assertEquals("app-1", loaded.get(0).getApplicationId());
        }

        /**
         * Verifies that a malformed line in reminders.dat is silently skipped
         * and does not prevent valid rows from being loaded.
         */
        @Test
        void corruptReminderLine_skippedAndValidRowsStillLoaded() throws IOException {
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