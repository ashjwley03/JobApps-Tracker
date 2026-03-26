# March Meet — JobApps Tracker

A desktop application for NUS/NTU students to track internship applications, interviews, deadlines, and offers — all in one place.

---

## Features

- **Application Dashboard** — view all applications with status, pay, location, and deadlines at a glance
- **Status Tracking** — track every stage: Applied → Interviewing → Offer → Accepted/Rejected
- **Interview Management** — log interview rounds, dates, and notes per application
- **Deadline Reminders** — never miss an offer acceptance deadline
- **Comparison Tool** — compare multiple offers side by side by salary, location, and job scope
- **Persistent Storage** — all data saved locally to plain text files, no database needed

---

## Team

| Name | Role |
|---|---|
| Nadia | UI Lead, Code Quality Lead |
| Yugam | Logic Lead, Testing Lead |
| Ashley | Storage Lead |

---

## Tech Stack

- **Language:** Java 17
- **UI Framework:** JavaFX
- **Build Tool:** Gradle
- **Testing:** JUnit 5

---

## Project Structure

```
JobApps-Tracker/
├── build.gradle
├── settings.gradle
├── docs/
│   └── API_DOCUMENTATION.md
└── src/
    ├── main/java/
    │   ├── gui/                  ← JavaFX controllers (Nadia)
    │   │   ├── Main.java
    │   │   ├── Launcher.java
    │   │   ├── MainController.java
    │   │   ├── DashboardController.java
    │   │   ├── CalendarController.java
    │   │   ├── CompareController.java
    │   │   └── NewApplicationController.java
    │   ├── logic/                ← Business logic (Yugam)
    │   │   ├── Application.java
    │   │   ├── ApplicationController.java
    │   │   ├── ApplicationStatus.java
    │   │   ├── Interview.java
    │   │   ├── InterviewController.java
    │   │   ├── Reminder.java
    │   │   ├── ReminderService.java
    │   │   └── ReminderType.java
    │   └── storage/              ← File persistence (Ashley)
    │       ├── Storage.java
    │       └── FileStorage.java
    ├── main/resources/view/      ← FXML views (Nadia)
    │   ├── MainWindow.fxml
    │   ├── DashboardView.fxml
    │   ├── CalendarView.fxml
    │   ├── CompareView.fxml
    │   ├── NewApplicationView.fxml
    │   └── styles.css
    └── test/java/
        ├── logic/                ← Logic tests (Yugam)
        │   ├── ApplicationControllerTest.java
        │   ├── InterviewControllerTest.java
        │   ├── ReminderServiceTest.java
        │   └── InMemoryStorage.java
        └── storage/              ← Storage tests (Ashley)
            └── FileStorageTest.java
```

---

## Running the App

### Prerequisites
- Java 17+
- Gradle (or use the Gradle wrapper)

### Build and Run
```bash
./gradlew run
```

### Run Tests
```bash
./gradlew test
```

### Build JAR
```bash
./gradlew jar
```

---

## Data Storage

All data is stored locally in a `data/` folder created automatically on first run:

```
data/
├── applications.dat
├── interviews.dat
└── reminders.dat
```

Each file stores one record per line, with fields separated by `|`. See `docs/API_DOCUMENTATION.md` for full format details.

---

## Documentation

Full API documentation including architecture diagrams, sequence diagrams, and data models is available in [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md).