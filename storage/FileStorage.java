package storage;

import logic.Application;
import logic.Interview;
import logic.Reminder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * File-based implementation of Storage.
 * Persists data in plain text files (one file per entity type).
 * No external libraries — uses only java.nio.file and standard Java.
 */
public class FileStorage implements Storage {

    private static final String SEP = "|";
    private static final String SAFE_SEP = "&#124;";  // escaped pipe for data containing |

    private final Path dataDir;
    private final Path applicationsFile;
    private final Path interviewsFile;
    private final Path remindersFile;

    public FileStorage() {
        this("data");
    }

    public FileStorage(String dataDir) {
        this.dataDir = Path.of(dataDir);
        this.applicationsFile = this.dataDir.resolve("applications.dat");
        this.interviewsFile = this.dataDir.resolve("interviews.dat");
        this.remindersFile = this.dataDir.resolve("reminders.dat");
    }

    @Override
    public void saveApplication(Application app) {
        ensureDataDir();
        List<Application> all = loadAllApplications();
        if (all.stream().noneMatch(a -> a.getId().equals(app.getId()))) {
            all.add(app);
            writeApplications(all);
        }
    }

    @Override
    public List<Application> loadAllApplications() {
        return readLines(applicationsFile).stream()
                .map(this::parseApplication)
                .filter(a -> a != null)
                .collect(Collectors.toList());
    }

    @Override
    public void updateApplication(Application app) {
        List<Application> all = loadAllApplications();
        all = all.stream()
                .map(a -> a.getId().equals(app.getId()) ? app : a)
                .collect(Collectors.toList());
        writeApplications(all);
    }

    @Override
    public void deleteApplication(String id) {
        List<Application> all = loadAllApplications().stream()
                .filter(a -> !a.getId().equals(id))
                .collect(Collectors.toList());
        writeApplications(all);
    }

    @Override
    public void saveInterview(Interview interview) {
        ensureDataDir();
        List<Interview> all = loadAllInterviews();
        if (all.stream().noneMatch(i -> i.getId().equals(interview.getId()))) {
            all.add(interview);
            writeInterviews(all);
        }
    }

    @Override
    public List<Interview> loadAllInterviews() {
        return readLines(interviewsFile).stream()
                .map(this::parseInterview)
                .filter(i -> i != null)
                .collect(Collectors.toList());
    }

    @Override
    public void updateInterview(Interview interview) {
        List<Interview> all = loadAllInterviews();
        all = all.stream()
                .map(i -> i.getId().equals(interview.getId()) ? interview : i)
                .collect(Collectors.toList());
        writeInterviews(all);
    }

    @Override
    public void saveReminder(Reminder reminder) {
        ensureDataDir();
        List<Reminder> all = loadAllReminders();
        if (all.stream().noneMatch(r -> r.getId().equals(reminder.getId()))) {
            all.add(reminder);
            writeReminders(all);
        }
    }

    @Override
    public List<Reminder> loadAllReminders() {
        return readLines(remindersFile).stream()
                .map(this::parseReminder)
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    @Override
    public void updateReminder(Reminder reminder) {
        List<Reminder> all = loadAllReminders();
        all = all.stream()
                .map(r -> r.getId().equals(reminder.getId()) ? reminder : r)
                .collect(Collectors.toList());
        writeReminders(all);
    }

    // --- Helpers ---

    private void ensureDataDir() {
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot create data directory: " + dataDir, e);
        }
    }

    private List<String> readLines(Path path) {
        try {
            if (!Files.exists(path)) return new ArrayList<>();
            return Files.readAllLines(path);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void writeApplications(List<Application> list) {
        List<String> lines = list.stream().map(this::formatApplication).collect(Collectors.toList());
        writeLines(applicationsFile, lines);
    }

    private void writeInterviews(List<Interview> list) {
        List<String> lines = list.stream().map(this::formatInterview).collect(Collectors.toList());
        writeLines(interviewsFile, lines);
    }

    private void writeReminders(List<Reminder> list) {
        List<String> lines = list.stream().map(this::formatReminder).collect(Collectors.toList());
        writeLines(remindersFile, lines);
    }

    private void writeLines(Path path, List<String> lines) {
        try {
            ensureDataDir();
            Files.write(path, lines);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write to " + path, e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace(SEP, SAFE_SEP);
    }

    private String unescape(String s) {
        if (s == null) return "";
        return s.replace(SAFE_SEP, SEP);
    }

    private String formatApplication(Application a) {
        return escape(a.getId()) + SEP + escape(a.getCompany()) + SEP
                + escape(a.getStatus()) + SEP + escape(a.getSalary()) + SEP + escape(a.getLocation());
    }

    private Application parseApplication(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 5) return null;
        return new Application(unescape(p[0]), unescape(p[1]), unescape(p[2]), unescape(p[3]), unescape(p[4]));
    }

    private String formatInterview(Interview i) {
        return escape(i.getId()) + SEP + escape(i.getApplicationId()) + SEP
                + i.getRound() + SEP + escape(i.getDateTime());
    }

    private Interview parseInterview(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 4) return null;
        try {
            return new Interview(unescape(p[0]), unescape(p[1]), Integer.parseInt(p[2]), unescape(p[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatReminder(Reminder r) {
        return escape(r.getId()) + SEP + escape(r.getDescription()) + SEP
                + escape(r.getDueDate()) + SEP + escape(r.getType());
    }

    private Reminder parseReminder(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 4) return null;
        return new Reminder(unescape(p[0]), unescape(p[1]), unescape(p[2]), unescape(p[3]));
    }
}
