package logic;

import storage.Storage;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ReminderService {
    private final Storage storage;

    public ReminderService(Storage storage) {
        this.storage = storage;
    }

    public Reminder addReminder(String applicationId, ReminderType type, LocalDate triggerDate) {
        Reminder reminder = new Reminder(applicationId, type, triggerDate);
        storage.saveReminder(reminder);
        return reminder;
    }

    public List<Reminder> getUpcomingReminders(int withinDays) {
        LocalDate cutoff = LocalDate.now().plusDays(withinDays);
        return storage.loadAllReminders().stream()
                .filter(r -> !r.isDismissed())
                .filter(r -> !r.getTriggerDate().isAfter(cutoff))
                .sorted((a, b) -> a.getTriggerDate().compareTo(b.getTriggerDate()))
                .collect(Collectors.toList());
    }

    public void dismissReminder(String reminderId) {
        storage.loadAllReminders().stream()
                .filter(r -> r.getId().equals(reminderId))
                .findFirst()
                .ifPresent(r -> {
                    r.dismiss();
                    storage.updateReminder(r);
                });
    }
}