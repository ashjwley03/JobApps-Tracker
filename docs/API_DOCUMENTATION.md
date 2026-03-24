# March Meet — API Documentation

**Version:** 1.0  
**Author:** Yugam  
**Last Updated:** March 2026  
**Stack:** Java

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [API Endpoint Map](#api-endpoint-map)
4. [Sequence Diagrams](#sequence-diagrams)
5. [Data Models](#data-models)
6. [Error Handling](#error-handling)

---

## Overview

March Meet is an internship application tracker that helps students manage their internship applications, interview schedules, deadlines, and offer comparisons from a single dashboard.

The system is divided into three layers:
- **UI Layer** — handles all user interactions (Nadia)
- **Logic Layer** — processes commands, validates input, and manages application state (Yugam)
- **Storage Layer** — persists data to disk (Ashley)

The Logic Layer acts as the bridge between UI and Storage. It exposes a clean internal API that the UI calls, and it delegates all data persistence to the Storage layer.

---

## System Architecture

```mermaid
graph TD
    User["👤 User (CLI / GUI)"]

    subgraph Frontend ["UI Layer (Nadia)"]
        Dashboard["Application Dashboard"]
        Comparison["Comparison Tool"]
        Calendar["Deadline & Interview Calendar"]
        DocVault["Document Vault"]
    end

    subgraph Backend ["Logic Layer (Yugam)"]
        AppController["Application Controller"]
        ReminderService["Reminder Service"]
        CompareService["Comparison Service"]
        AuthService["Auth Service"]
    end

    subgraph Database ["Storage Layer (Ashley)"]
        AppTable[("Applications")]
        UserTable[("Users")]
        DocTable[("Documents")]
        ReminderTable[("Reminders")]
    end

    User --> Frontend
    Dashboard --> AppController
    Comparison --> CompareService
    Calendar --> ReminderService
    DocVault --> AppController

    AppController --> AppTable
    AppController --> DocTable
    ReminderService --> ReminderTable
    AuthService --> UserTable
    CompareService --> AppTable
```

---

## API Endpoint Map

These represent the internal method calls between layers (not HTTP endpoints, since this is a Java desktop app).

```mermaid
graph LR
    API["Logic API"]

    API --> A["ApplicationController"]
    A --> A1["addApplication(company, role, pay, location, status)"]
    A --> A2["getAllApplications()"]
    A --> A3["getApplicationById(id)"]
    A --> A4["updateStatus(id, newStatus)"]
    A --> A5["deleteApplication(id)"]
    A --> A6["compareApplications(id1, id2, ...)"]

    API --> B["InterviewController"]
    B --> B1["addInterview(applicationId, date, round)"]
    B --> B2["getInterviewsByApplication(applicationId)"]
    B --> B3["updateInterviewNotes(interviewId, notes)"]

    API --> C["ReminderService"]
    C --> C1["getUpcomingReminders()"]
    C --> C2["addReminder(applicationId, deadline, type)"]
    C --> C3["dismissReminder(reminderId)"]

    API --> D["DocumentController"]
    D --> D1["uploadDocument(name, filePath, type)"]
    D --> D2["getDocuments()"]
    D --> D3["deleteDocument(docId)"]
```

---

## Sequence Diagrams

### 1. Add New Application

```mermaid
sequenceDiagram
    actor User
    participant UI as UI Layer
    participant Logic as Logic Layer
    participant Storage as Storage Layer

    User->>UI: Fills in application form
    UI->>Logic: addApplication(company, role, pay, location, status)
    Logic->>Logic: Validate fields (non-null, valid status)
    Logic->>Storage: save(application)
    Storage-->>Logic: Return saved Application object
    Logic-->>UI: Return success + Application
    UI-->>User: Show new entry in dashboard

    Note over Logic,Storage: If deadline is provided...
    Logic->>Storage: saveReminder(applicationId, deadline)
    Storage-->>Logic: Reminder saved
    Logic-->>UI: Reminder added
    UI-->>User: Deadline badge shown on card
```

### 2. Update Application Status

```mermaid
sequenceDiagram
    actor User
    participant UI as UI Layer
    participant Logic as Logic Layer
    participant Storage as Storage Layer

    User->>UI: Clicks status dropdown → selects new status
    UI->>Logic: updateStatus(applicationId, newStatus)
    Logic->>Logic: Validate status is one of [APPLIED, INTERVIEWING, OFFER, REJECTED, ACCEPTED]
    Logic->>Storage: update(applicationId, newStatus)
    Storage-->>Logic: Updated Application
    Logic-->>UI: Success
    UI-->>User: Dashboard refreshes with new status
```

### 3. Compare Applications

```mermaid
sequenceDiagram
    actor User
    participant UI as UI Layer
    participant Logic as Logic Layer
    participant Storage as Storage Layer

    User->>UI: Selects 2+ applications to compare
    UI->>Logic: compareApplications([id1, id2, ...])
    Logic->>Storage: getApplicationsByIds([id1, id2, ...])
    Storage-->>Logic: List of Application objects
    Logic->>Logic: Build comparison summary (salary, location, scope)
    Logic-->>UI: ComparisonResult object
    UI-->>User: Side-by-side comparison view
```

### 4. Reminder Fires (Deadline Alert)

```mermaid
sequenceDiagram
    participant Scheduler as Reminder Scheduler
    participant Logic as Logic Layer
    participant Storage as Storage Layer
    participant UI as UI Layer

    Scheduler->>Logic: checkUpcomingDeadlines()
    Logic->>Storage: getRemindersWithin(48 hours)
    Storage-->>Logic: List of due Reminders
    Logic-->>UI: triggerAlert(reminders)
    UI-->>UI: Show notification / banner to user
```

---

## Data Models

### Application

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Unique identifier (UUID) |
| `companyName` | `String` | Name of the company |
| `roleTitle` | `String` | Job/internship title |
| `pay` | `double` | Monthly salary |
| `location` | `String` | Office location |
| `status` | `ApplicationStatus` | Enum: APPLIED, INTERVIEWING, OFFER, REJECTED, ACCEPTED |
| `dateApplied` | `LocalDate` | Date application was submitted |
| `deadline` | `LocalDate` | Offer acceptance deadline (nullable) |
| `notes` | `String` | General remarks / job scope notes |

### Interview

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Unique identifier |
| `applicationId` | `String` | FK to Application |
| `round` | `int` | Interview round number (1, 2, 3...) |
| `date` | `LocalDateTime` | Scheduled date and time |
| `notes` | `String` | Notes on interviewer, questions asked |

### Reminder

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Unique identifier |
| `applicationId` | `String` | FK to Application |
| `type` | `ReminderType` | Enum: DEADLINE, INTERVIEW, FOLLOWUP |
| `triggerDate` | `LocalDate` | When to alert the user |
| `dismissed` | `boolean` | Whether user has dismissed it |

### Document

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Unique identifier |
| `name` | `String` | Display name (e.g. "Resume_v3") |
| `filePath` | `String` | Path to file on disk |
| `type` | `DocumentType` | Enum: RESUME, TRANSCRIPT, ID, OTHER |

---

## Error Handling

| Error | Cause | Behaviour |
|---|---|---|
| `InvalidStatusException` | Status value not in enum | Logic layer rejects, UI shows error message |
| `ApplicationNotFoundException` | ID doesn't exist in storage | Logic throws, UI shows "Application not found" |
| `MissingFieldException` | Required field is null/empty | Validation fails before storage call |
| `StorageException` | File read/write failure | Logic catches, returns failure result to UI |

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
