## JobApps Tracker

**Version:** V1.5 — Release Candidate
A desktop application for NUS/NTU students to track internship applications, interviews, deadlines, and offers — all in one place.

---

## Features

- **Application Dashboard** — view all applications with status, pay, location, and deadlines at a glance, with bar and pie charts
- **Status Tracking** — track every stage: Applied → Interviewing → Offer → Accepted/Rejected/Withdrawn, with enforced transition rules
- **Interview Management** — log interview rounds, dates, and notes per application, with referential integrity checks
- **Deadline Reminders** — never miss an offer acceptance deadline, with calendar-based reminder view
- **Comparison Tool** — compare multiple offers side by side by salary, location, and job scope; highest pay highlighted automatically
- **Search** — search applications by company or role name from the dashboard
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
- **UI Framework:** JavaFX 21
- **Build Tool:** Gradle 9.3
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
    │   ├── gui/                        ← JavaFX controllers (Nadia)
    │   │   ├── Launcher.java
    │   │   ├── Main.java
    │   │   ├── MainController.java
    │   │   ├── DashboardController.java
    │   │   ├── CalendarController.java
    │   │   ├── CompareController.java
    │   │   └── NewApplicationController.java
    │   ├── logic/                      ← Business logic (Yugam)
    │   │   ├── Application.java
    │   │   ├── ApplicationController.java
    │   │   ├── ApplicationStatus.java
    │   │   ├── Interview.java
    │   │   ├── InterviewController.java
    │   │   ├── Reminder.java
    │   │   ├── ReminderService.java
    │   │   └── ReminderType.java
    │   └── storage/                    ← File persistence (Ashley)
    │       ├── Storage.java
    │       └── FileStorage.java
    ├── main/resources/view/            ← FXML views + CSS (Nadia)
    │   ├── MainWindow.fxml
    │   ├── DashboardView.fxml
    │   ├── CalendarView.fxml
    │   ├── CompareView.fxml
    │   ├── NewApplicationView.fxml
    │   └── styles.css
    └── test/java/
        ├── logic/                      ← Logic tests (Yugam)
        │   ├── ApplicationControllerTest.java
        │   ├── InterviewControllerTest.java
        │   ├── ReminderServiceTest.java
        │   └── InMemoryStorage.java
        └── storage/                    ← Storage tests (Ashley)
            └── FileStorageTest.java
```

---

## Running the App

### Prerequisites
- Java 17+

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

The JAR will be output to `build/libs/`.

---

## Test Coverage

| Test Class | Tests | Status |
|---|---|---|
| `CalendarControllerTest` | 6 | ✅ Passing |
| `CompareControllerTest` | 4 | ✅ Passing |
| `DashboardControllerTest` | 8 | ✅ Passing |
| `NewApplicationControllerTest` | 8 | ✅ Passing |
| `ApplicationControllerTest` | 12 | ✅ Passing |
| `InterviewControllerTest` | 4 | ✅ Passing |
| `ReminderServiceTest` | 5 | ✅ Passing |
| `FileStorageTest` | 40 | ✅ Passing |
| **Total** | **87** | **100% passing** |

---

## Data Storage

All data is stored locally in a `data/` folder created automatically on first run:

```
data/
├── applications.dat
├── interviews.dat
└── reminders.dat
```

Each file stores one record per line with fields separated by `|`. Pipe characters in user input are escaped as `&#124;` to prevent data corruption. Corrupted lines are silently skipped and logged without crashing the app. See [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md) for full format details.

---

## Known Limitations (Not Yet Implemented)

The following features from the original PRD are **not yet implemented** and are planned for future iterations:

- **Document Vault** — upload and attach resumes, cover letters, or offer letters to applications
- **Travel Time** — estimate commute time to office locations
- **Job Scope Filtering** — filter or search applications by job scope, salary range, or location
- **Calendar Date Drill-Down** — click a date on the calendar to view detailed events for that day
- **Offer Deadline Setting from UI** — set or edit offer acceptance deadlines directly from the dashboard
- **Follow-Up Reminder Notes** — attach custom notes or target dates to follow-up reminders

---

## Documentation

Full API documentation including architecture diagrams, class diagrams, sequence diagrams, and data models is available in [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md).
