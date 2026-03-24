package storage;

import logic.Application;
import logic.Interview;
import logic.Reminder;

import java.util.List;

/**
 * Storage interface — defines the contract between Logic (Yugam) and Storage (Ashley).
 * Ashley: implement this interface with your actual file/DB storage.
 */
public interface Storage {
    void saveApplication(Application app);
    List<Application> loadAllApplications();
    void updateApplication(Application app);
    void deleteApplication(String id);

    void saveInterview(Interview interview);
    List<Interview> loadAllInterviews();
    void updateInterview(Interview interview);

    void saveReminder(Reminder reminder);
    List<Reminder> loadAllReminders();
    void updateReminder(Reminder reminder);
}
