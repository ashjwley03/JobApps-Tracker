# March Meet — API Documentation

**Version:** 2.0  
**Author:** Yugam  
**Last Updated:** March 2026  
**Stack:** Java 17, JavaFX, Gradle

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [API Endpoint Map](#api-endpoint-map)
4. [Sequence Diagrams](#sequence-diagrams)
5. [Data Models](#data-models)
6. [Storage Format](#storage-format)
7. [Error Handling](#error-handling)
8. [Status Flow](#status-flow)

---

## Overview

March Meet is a desktop internship application tracker built with JavaFX. It helps students manage applications, interview schedules, deadlines, and offer comparisons from a single dashboard.

The system is divided into three layers:
- **GUI Layer** — JavaFX controllers and FXML views (Nadia)
- **Logic Layer** — Application logic, validation, and business rules (Yugam)
- **Storage Layer** — File-based persistence using plain text `.dat` files (Ashley)

---

## System Architecture

```mermaid
graph TD
    User["👤 User (JavaFX Desktop App)"]

    subgraph GUI ["GUI Layer (Nadia)"]
        Main["MainController"]
        Dashboard["DashboardController"]
        Calendar["CalendarController"]
        Compare["CompareController"]
        NewApp["NewApplicationController"]
    end

    subgraph Logic ["Logic Layer (Yugam)"]
        AppController["ApplicationController"]
        InterviewController["InterviewController"]
        ReminderService["ReminderService"]
    end

    subgraph Storage ["Storage Layer (Ashley)"]
        Interface["Storage (interface)"]
        FileStorage["FileStorage"]
        DataFiles[("data/\napplications.dat\ninterviews.dat\nreminders.dat")]
    end

    User --> Main
    Main --> Dashboard
    Main --> Calendar
    Main --> Compare
    Main --> NewApp

    Dashboard --> AppController
    Compare --> AppController
    Calendar --> ReminderService
    NewApp --> AppController

    AppController --> Interface
    InterviewController --> Interface
    ReminderService --> Interface
    Interface --> FileStorage
    FileStorage --> DataFiles
```

---

## API Endpoint Map

Internal method-level API between layers.

```mermaid
graph LR
    Logic["Logic Layer API"]

    Logic --> A["ApplicationController"]
    A --> A1["addApplication(company, role, pay, location, status)"]
    A --> A2["getAllApplications()"]
    A --> A3["getApplicationById(id)"]
    A --> A4["updateStatus(id, newStatus)"]
    A --> A5["deleteApplication(id)"]
    A --> A6["compareApplications(ids)"]
    A --> A7["filterByStatus(status)"]

    Logic --> B["InterviewController"]
    B --> B1["addInterview(applicationId, round, date)"]
    B --> B2["getInterviewsByApplication(applicationId)"]
    B --> B3["updateNotes(interviewId, notes)"]

    Logic --> C["ReminderService"]
    C --> C1["addReminder(applicationId, type, triggerDate)"]
    C --> C2["getUpcomingReminders(withinDays)"]
    C --> C3["dismissReminder(reminderId)"]

    Logic --> D["Storage (interface)"]
    D --> D1["FileStorage (Ashley's implementation)"]
    D --> D2["InMemoryStorage (test stub)"]
```

---

## Sequence Diagrams

### 1. Add New Application

```mermaid
sequenceDiagram
    actor User
    participant UI as GUI Layer
    participant Logic as ApplicationController
    participant Storage as FileStorage

    User->>UI: Fills NewApplicationView form
    UI->>Logic: addApplication(company, role, pay, location, status)
    Logic->>Logic: Validate fields (non-null, non-blank)
    Logic->>Storage: saveApplication(app)
    Storage->>Storage: Append to applications.dat
    Storage-->>Logic: OK
    Logic-->>UI: Return Application object
    UI-->>User: Navigate back to Dashboard
```

### 2. Load Dashboard

```mermaid
sequenceDiagram
    actor User
    participant UI as DashboardController
    participant Logic as ApplicationController
    participant Storage as FileStorage

    User->>UI: Opens app / clicks Dashboard
    UI->>Logic: getAllApplications()
    Logic->>Storage: loadAllApplications()
    Storage->>Storage: Read applications.dat, parse each line
    Storage-->>Logic: List of Application objects
    Logic-->>UI: List of Application objects
    UI->>UI: Populate stat cards, bar chart, pie chart, table
    UI-->>User: Dashboard rendered
```

### 3. Compare Applications

```mermaid
sequenceDiagram
    actor User
    participant UI as CompareController
    participant Logic as ApplicationController
    participant Storage as FileStorage

    User->>UI: Selects applications to compare
    UI->>Logic: compareApplications([id1, id2, ...])
    Logic->>Storage: loadAllApplications()
    Storage-->>Logic: All applications
    Logic->>Logic: Filter by ids, sort by pay descending
    Logic-->>UI: Sorted list of Applications
    UI-->>User: Side-by-side comparison view
```

### 4. Update Application Status

```mermaid
sequenceDiagram
    actor User
    participant UI as DashboardController
    participant Logic as ApplicationController
    participant Storage as FileStorage

    User->>UI: Changes status on application
    UI->>Logic: updateStatus(id, newStatus)
    Logic->>Logic: getApplicationById(id)
    Logic->>Storage: loadAllApplications()
    Storage-->>Logic: All applications
    Logic->>Logic: Set new status on matching app
    Logic->>Storage: updateApplication(app)
    Storage->>Storage: Rewrite applications.dat
    Storage-->>Logic: OK
    Logic-->>UI: Updated Application
    UI-->>User: Dashboard refreshes
```

---

## Data Models

### Application

| Field | Type | Description |
|---|---|---|
| `id` | `String` | UUID — unique identifier |
| `companyName` | `String` | Name of the company |
| `roleTitle` | `String` | Job/internship title |
| `pay` | `double` | Monthly salary |
| `location` | `String` | Office location |
| `status` | `ApplicationStatus` | Enum: APPLIED, INTERVIEWING, OFFER, REJECTED, ACCEPTED |
| `dateApplied` | `LocalDate` | Date application was submitted |
| `deadline` | `LocalDate` | Offer acceptance deadline (nullable) |
| `notes` | `String` | Remarks / job scope notes |

### Interview

| Field | Type | Description |
|---|---|---|
| `id` | `String` | UUID — unique identifier |
| `applicationId` | `String` | FK to Application |
| `round` | `int` | Interview round number (1, 2, 3...) |
| `date` | `LocalDateTime` | Scheduled date and time |
| `notes` | `String` | Notes on interviewer, questions asked |

### Reminder

| Field | Type | Description |
|---|---|---|
| `id` | `String` | UUID — unique identifier |
| `applicationId` | `String` | FK to Application |
| `type` | `ReminderType` | Enum: DEADLINE, INTERVIEW, FOLLOWUP |
| `triggerDate` | `LocalDate` | When to alert the user |
| `dismissed` | `boolean` | Whether user has dismissed it |

---

## Storage Format

Ashley's `FileStorage` persists data in plain text `.dat` files in the `data/` directory. Each line is one record, fields separated by `|`. Pipe characters in field values are escaped as `&#124;`.

### applications.dat
```
id|companyName|roleTitle|pay|location|status|dateApplied|deadline|notes
```
Example:
```
abc12345-...|Google|SWE Intern|5000.0|Singapore|APPLIED|2026-03-01|2026-04-01|Great role
```

### interviews.dat
```
id|applicationId|round|date|notes
```
Example:
```
def67890-...|abc12345-...|1|2026-03-15T10:00|Very friendly interviewer
```

### reminders.dat
```
id|applicationId|type|triggerDate|dismissed
```
Example:
```
ghi11111-...|abc12345-...|DEADLINE|2026-04-01|false
```

---

## Error Handling

| Error | Cause | Behaviour |
|---|---|---|
| `IllegalArgumentException` | Null/blank company name or role title | Logic rejects before storage call |
| `IllegalArgumentException` | ID not found in storage | Thrown by `getApplicationById`, `updateNotes` |
| `RuntimeException` | Cannot create data directory | Thrown by `FileStorage.ensureDataDir()` |
| `RuntimeException` | Cannot write to `.dat` file | Thrown by `FileStorage.writeLines()` |
| Corrupt line in `.dat` file | Parse error | Silently skipped, returns null, filtered out |

---

## Status Flow

```mermaid
stateDiagram-v2
    [*] --> APPLIED
    APPLIED --> INTERVIEWING
    APPLIED --> REJECTED
    INTERVIEWING --> OFFER
    INTERVIEWING --> REJECTED
    OFFER --> ACCEPTED
    OFFER --> REJECTED
```