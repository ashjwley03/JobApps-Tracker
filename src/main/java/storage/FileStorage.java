package storage;

import logic.Application;
import logic.ApplicationStatus;
import logic.Interview;
import logic.Reminder;
import logic.ReminderType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * File-based implementation of the Storage interface.
 * This class handles the serialization and deserialization of application data
 * into plain-text files using a pipe-delimited format.
 */
public class FileStorage implements Storage {

    private static final Logger LOGGER = Logger.getLogger(FileStorage.class.getName());
    private static final String SEP = "|";
    private static final String SAFE_SEP = "&#124;"; // HTML entity for pipe to prevent corruption

    private final Path dataDir;
    private final Path applicationsFile;
    private final Path interviewsFile;
    private final Path remindersFile;

    /**
     * Default constructor using the standard "data" directory.
     */
    public FileStorage() {
        this("data");
    }

    /**
     * Initializes storage with a specific directory path.
     * @param dataDir The directory where data files will be stored.
     */
    public FileStorage(String dataDir) {
        this.dataDir = Path.of(dataDir);
        this.applicationsFile = this.dataDir.resolve("applications.dat");
        this.interviewsFile = this.dataDir.resolve("interviews.dat");
        this.remindersFile = this.dataDir.resolve("reminders.dat");
    }

    /**
     * Saves a new application to the storage file.
     * Prevents saving duplicate applications with the same ID.
     *
     * @param app The application to save.
     */
    @Override
    public void saveApplication(Application app) {
        this.ensureDataDir();
        List<Application> all = this.loadAllApplications();

        boolean isDuplicate = all.stream().anyMatch(a -> {
            return a.getId().equals(app.getId());
        });

        if (!isDuplicate) {
            all.add(app);
            this.writeApplications(all);
        }
    }

    /**
     * Loads all saved applications from the storage file.
     * Corrupted lines are skipped automatically.
     *
     * @return A list of all valid stored applications.
     */
    @Override
    public List<Application> loadAllApplications() {
        return this.readLines(this.applicationsFile).stream()
                .map(line -> {
                    return this.parseApplication(line);
                })
                .filter(a -> {
                    return a != null;
                })
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing application in the storage file.
     *
     * @param app The application with updated data.
     */
    @Override
    public void updateApplication(Application app) {
        List<Application> all = this.loadAllApplications();
        List<Application> updatedList = all.stream()
                .map(a -> {
                    return a.getId().equals(app.getId()) ? app : a;
                })
                .collect(Collectors.toList());
        this.writeApplications(updatedList);
    }

    /**
     * Deletes an application from the storage file by its ID.
     *
     * @param id The ID of the application to delete.
     */
    @Override
    public void deleteApplication(String id) {
        List<Application> all = this.loadAllApplications().stream()
                .filter(a -> {
                    return !a.getId().equals(id);
                })
                .collect(Collectors.toList());
        this.writeApplications(all);
    }

    /**
     * Loads all saved interviews from the storage file.
     * Corrupted lines are skipped automatically.
     *
     * @return A list of all valid stored interviews.
     */
    @Override
    public List<Interview> loadAllInterviews() {
        return this.readLines(this.interviewsFile).stream()
                .map(line -> {
                    return this.parseInterview(line);
                })
                .filter(i -> {
                    return i != null;
                })
                .collect(Collectors.toList());
    }

    /**
     * Saves a new interview record to the storage file.
     *
     * @param interview The interview to save.
     */
    @Override
    public void saveInterview(Interview interview) {
        this.ensureDataDir();
        List<Interview> all = this.loadAllInterviews();
        boolean isDuplicate = all.stream().anyMatch(i -> i.getId().equals(interview.getId()));
        if (!isDuplicate) {
            all.add(interview);
            this.writeInterviews(all);
        }
    }

    /**
     * Updates an existing interview record in the storage file.
     *
     * @param interview The interview with updated data.
     */
    @Override
    public void updateInterview(Interview interview) {
        List<Interview> all = this.loadAllInterviews();
        List<Interview> updatedList = all.stream()
                .map(i -> {
                    return i.getId().equals(interview.getId()) ? interview : i;
                })
                .collect(Collectors.toList());
        this.writeInterviews(updatedList);
    }

    /**
     * Loads all saved reminders from the storage file.
     * Corrupted lines are skipped automatically.
     *
     * @return A list of all valid stored reminders.
     */
    @Override
    public List<Reminder> loadAllReminders() {
        return this.readLines(this.remindersFile).stream()
                .map(line -> {
                    return this.parseReminder(line);
                })
                .filter(r -> {
                    return r != null;
                })
                .collect(Collectors.toList());
    }

    /**
     * Saves a new reminder to the storage file.
     *
     * @param reminder The reminder to save.
     */
    @Override
    public void saveReminder(Reminder reminder) {
        this.ensureDataDir();
        List<Reminder> all = this.loadAllReminders();
        boolean isDuplicate = all.stream().anyMatch(r -> r.getId().equals(reminder.getId()));
        if (!isDuplicate) {
            all.add(reminder);
            this.writeReminders(all);
        }
    }

    /**
     * Updates an existing reminder in the storage file.
     *
     * @param reminder The reminder with updated data.
     */
    @Override
    public void updateReminder(Reminder reminder) {
        List<Reminder> all = this.loadAllReminders();
        List<Reminder> updatedList = all.stream()
                .map(r -> {
                    return r.getId().equals(reminder.getId()) ? reminder : r;
                })
                .collect(Collectors.toList());
        this.writeReminders(updatedList);
    }

    // --- Core Persistence Helpers ---

    /**
     * Ensures that the data directory exists before attempting to write files.
     * @throws RuntimeException if the directory cannot be created.
     */
    private void ensureDataDir() {
        try {
            if (!Files.exists(this.dataDir)) {
                Files.createDirectories(this.dataDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("CRITICAL: Data directory creation failed: " + this.dataDir, e);
        }
    }

    /**
     * Reads all lines from a given file path.
     * Logs the error and throws a controlled exception if reading fails, preventing silent data loss.
     *
     * @param path The file path to read from.
     * @return A list of string lines from the file.
     * @throws RuntimeException if a file access error occurs.
     */
    private List<String> readLines(Path path) {
        try {
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            return Files.readAllLines(path);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read data from: " + path, e);
            throw new RuntimeException("Error accessing storage file. Please check file permissions.", e);
        }
    }

    /**
     * Writes a list of formatted string lines to a specified file path.
     *
     * @param path  The file path to write to.
     * @param lines The lines to write.
     * @throws RuntimeException if a file access error occurs.
     */
    private void writeLines(Path path, List<String> lines) {
        try {
            this.ensureDataDir();
            Files.write(path, lines);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write data to: " + path, e);
            throw new RuntimeException("Failed to save data. Storage may be full or inaccessible.", e);
        }
    }

    /**
     * Escapes the delimiter character in user input to prevent file format corruption.
     *
     * @param s The string to escape.
     * @return The escaped string.
     */
    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(SEP, SAFE_SEP);
    }

    /**
     * Restores escaped delimiter characters back to their original form after reading.
     *
     * @param s The string to unescape.
     * @return The unescaped string.
     */
    private String unescape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(SAFE_SEP, SEP);
    }

    // --- Formatting Logic ---

    /**
     * Serializes an Application object into a pipe-delimited string.
     */
    private String formatApplication(Application a) {
        return this.escape(a.getId()) + SEP
                + this.escape(a.getCompanyName()) + SEP
                + this.escape(a.getRoleTitle()) + SEP
                + a.getPay() + SEP
                + this.escape(a.getLocation()) + SEP
                + a.getStatus().name() + SEP
                + a.getDateApplied().toString() + SEP
                + (a.getDeadline() != null ? a.getDeadline().toString() : "") + SEP
                + this.escape(a.getNotes());
    }

    /**
     * Deserializes a pipe-delimited string back into an Application object.
     * Returns null if the line is corrupted or fails parsing.
     */
    private Application parseApplication(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] p = line.split("\\|", -1);
        if (p.length < 9) {
            return null;
        }
        try {
            double pay = Double.parseDouble(this.unescape(p[3]));
            ApplicationStatus status = ApplicationStatus.valueOf(this.unescape(p[5]));
            LocalDate dateApplied = LocalDate.parse(this.unescape(p[6]));
            LocalDate deadline = (p[7] == null || p[7].isBlank()) ? null : LocalDate.parse(this.unescape(p[7]));

            return new Application(
                    this.unescape(p[0]), this.unescape(p[1]), this.unescape(p[2]),
                    pay, this.unescape(p[4]), status, dateApplied, deadline, this.unescape(p[8])
            );
        } catch (DateTimeParseException | IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Skipping corrupted application record line.", e);
            return null;
        }
    }

    /** Helper to serialize and write the entire application list. */
    private void writeApplications(List<Application> list) {
        List<String> lines = list.stream().map(this::formatApplication).collect(Collectors.toList());
        this.writeLines(this.applicationsFile, lines);
    }

    /** Helper to serialize and write the entire interview list. */
    private void writeInterviews(List<Interview> list) {
        List<String> lines = list.stream().map(this::formatInterview).collect(Collectors.toList());
        this.writeLines(this.interviewsFile, lines);
    }

    /** Helper to serialize and write the entire reminder list. */
    private void writeReminders(List<Reminder> list) {
        List<String> lines = list.stream().map(this::formatReminder).collect(Collectors.toList());
        this.writeLines(this.remindersFile, lines);
    }

    /**
     * Serializes an Interview object into a pipe-delimited string.
     */
    private String formatInterview(Interview i) {
        return this.escape(i.getId()) + SEP
                + this.escape(i.getApplicationId()) + SEP
                + i.getRound() + SEP
                + i.getDate().toString() + SEP
                + this.escape(i.getNotes());
    }

    /**
     * Deserializes a pipe-delimited string back into an Interview object.
     * Returns null if the line is corrupted or fails parsing.
     */
    private Interview parseInterview(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] p = line.split("\\|", -1);
        if (p.length < 5) {
            return null;
        }
        try {
            LocalDateTime date = LocalDateTime.parse(this.unescape(p[3]));
            return new Interview(this.unescape(p[0]), this.unescape(p[1]), Integer.parseInt(p[2]), date, this.unescape(p[4]));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Serializes a Reminder object into a pipe-delimited string.
     */
    private String formatReminder(Reminder r) {
        return this.escape(r.getId()) + SEP
                + this.escape(r.getApplicationId()) + SEP
                + r.getType().name() + SEP
                + r.getTriggerDate().toString() + SEP
                + r.isDismissed();
    }

    /**
     * Deserializes a pipe-delimited string back into a Reminder object.
     * Returns null if the line is corrupted or fails parsing.
     */
    private Reminder parseReminder(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] p = line.split("\\|", -1);
        if (p.length < 5) {
            return null;
        }
        try {
            ReminderType type = ReminderType.valueOf(this.unescape(p[2]));
            LocalDate triggerDate = LocalDate.parse(this.unescape(p[3]));
            boolean dismissed = Boolean.parseBoolean(this.unescape(p[4]));
            return new Reminder(this.unescape(p[0]), this.unescape(p[1]), type, triggerDate, dismissed);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return null;
        }
    }
}