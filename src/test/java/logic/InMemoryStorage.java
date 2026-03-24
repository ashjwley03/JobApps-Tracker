package logic;

import storage.Storage;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory Storage stub for testing only.
 * Replace with Ashley's real implementation when ready.
 */
public class InMemoryStorage implements Storage {
    private final List<Application> applications = new ArrayList<>();
    private final List<Interview> interviews = new ArrayList<>();
    private final List<Reminder> reminders = new ArrayList<>();

    @Override
    public void saveApplication(Application app) { applications.add(app); }

    @Override
    public List<Application> loadAllApplications() { return new ArrayList<>(applications); }

    @Override
    public void updateApplication(Application app) {
        applications.removeIf(a -> a.getId().equals(app.getId()));
        applications.add(app);
    }

    @Override
    public void deleteApplication(String id) {
        applications.removeIf(a -> a.getId().equals(id));
    }

    @Override
    public void saveInterview(Interview interview) { interviews.add(interview); }

    @Override
    public List<Interview> loadAllInterviews() { return new ArrayList<>(interviews); }

    @Override
    public void updateInterview(Interview interview) {
        interviews.removeIf(i -> i.getId().equals(interview.getId()));
        interviews.add(interview);
    }

    @Override
    public void saveReminder(Reminder reminder) { reminders.add(reminder); }

    @Override
    public List<Reminder> loadAllReminders() { return new ArrayList<>(reminders); }

    @Override
    public void updateReminder(Reminder reminder) {
        reminders.removeIf(r -> r.getId().equals(reminder.getId()));
        reminders.add(reminder);
    }
}