# March Meet — System Architecture & API Documentation

**Version:** 3.0 (V2.0 Release)
**Author:** Yugam
**Last Updated:** April 16, 2026
**Stack:** Java 17, JavaFX 21, Gradle 9.3

> All diagrams are authored in PlantUML (`.puml` files in this directory) and exported as PNG.
> To regenerate any diagram, run: `plantuml -tpng <file>.puml -o .`

---

## Table of Contents
1. [System Architecture](#1-system-architecture)
2. [Class Diagram](#2-class-diagram)
3. [State Diagram (Status Flow)](#3-state-diagram-status-flow)
4. [Activity Diagram](#4-activity-diagram)
5. [Sequence Diagrams](#5-sequence-diagrams)
6. [API Endpoint Map](#6-api-endpoint-map)
7. [Storage Format & Security](#7-storage-format--security)
8. [Error & Exception Handling](#8-error--exception-handling)

---

## 1. System Architecture

The application strictly adheres to a 3-Tier Layered Architecture to ensure separation of concerns. The GUI layer strictly consumes the Logic layer's API and never interacts directly with the Storage layer.

**Source:** `SystemArchitecture.puml`

![System Architecture Diagram](System%20Architecture%20Diagram.png)

---

## 2. Class Diagram

This diagram maps the core domain models and their relationships with the Logic layer controllers and the Storage interface.

**Source:** `ClassDiagram.puml`

![Class Diagram](ClassDiagram.png)

---

## 3. State Diagram (Status Flow)

This state machine represents the valid application status transitions enforced by the `ApplicationController`. `REJECTED`, `ACCEPTED`, and `WITHDRAWN` are strictly enforced terminal states — no further transitions are permitted once reached.

**Source:** `StateDiagram.puml`

![State Diagram](StateDiagram.png)

---

## 4. Activity Diagram

This diagram shows the main user workflow through the application, covering all major actions: adding, editing, deleting, searching, comparing, and browsing the calendar.

**Source:** `ActivityDiagram.puml`

![Activity Diagram](Activity%20Diagram.png)

---

## 5. Sequence Diagrams

### 5.1 Add New Application

**Source:** `SequenceDiagram-AddNewApplication.puml`

![Sequence Diagram — Add New Application](SequenceDiagram-AddNewApplication.png)

### 5.2 Load Dashboard

**Source:** `SequenceDiagram-LoadDashboard.puml`

![Sequence Diagram — Load Dashboard](SequenceDiagram-LoadDashboard.png)

### 5.3 Edit Application Details

When the user clicks the Edit button on a dashboard row, the application's details are loaded into the edit form where the user can update fields and save.

**Source:** `SequenceDiagram-EditApplication.puml`

![Sequence Diagram — Edit Application](SequenceDiagram-EditApplication.png)

### 5.4 Add Interview with Referential Integrity Check

When a new interview is added, the Logic layer verifies the parent application exists before saving, preventing orphaned records.

**Source:** `SequenceDiagram-AddInterview.puml`

![Sequence Diagram — Add Interview](SequenceDiagram-AddInterview.png)

### 5.5 Invalid Status Transition

The Logic layer blocks invalid status updates before they reach storage, throwing a typed exception for the GUI to handle.

**Source:** `SequenceDiagram-InvalidStatusTransition.puml`

![Sequence Diagram — Invalid Status Transition](SequenceDiagram-InvalidStatusTransition.png)

### 5.6 Compare Applications

**Source:** `SequenceDiagram-CompareApplications.puml`

![Sequence Diagram — Compare Applications](SequenceDiagram-CompareApplications.png)

---

## 6. API Endpoint Map

Internal method-level API exposed by the Logic layer for the GUI layer to consume.

### `ApplicationController`
- `Application addApplication(company, role, pay, location, status)` — throws `IllegalArgumentException` on blank/null company name or role title
- `List<Application> getAllApplications()`
- `Application getApplicationById(id)` — throws `IllegalArgumentException` if not found
- `Application updateStatus(id, newStatus)` — throws `IllegalStateException` if current status is `REJECTED`, `ACCEPTED`, or `WITHDRAWN` (terminal states), or if jumping from `APPLIED` directly to `OFFER`
- `Application updateDetails(id, companyName, roleTitle, pay, location)` — throws `IllegalArgumentException` on blank/null company name or role title
- `Application updateDeadline(id, deadline)` — accepts `null` to clear the deadline
- `Application updateNotes(id, notes)` — stores empty string if `null` is passed
- `void deleteApplication(id)` — throws `IllegalArgumentException` if not found
- `List<Application> compareApplications(List<String> ids)` — sorted by pay descending
- `List<Application> filterByStatus(ApplicationStatus status)` — returns all applications matching the given status

### `InterviewController`
- `Interview addInterview(applicationId, round, date)` — throws `IllegalArgumentException` if parent application does not exist (referential integrity)
- `List<Interview> getAllInterviews()`
- `List<Interview> getInterviewsByApplication(applicationId)` — sorted by round ascending
- `Interview updateNotes(interviewId, notes)` — throws `IllegalArgumentException` if interview not found

### `ReminderService`
- `Reminder addReminder(applicationId, type, triggerDate)` — throws `IllegalArgumentException` if parent application does not exist (referential integrity)
- `List<Reminder> getUpcomingReminders(withinDays)` — filters dismissed and expired automatically, sorted by date ascending
- `void dismissReminder(reminderId)` — silently no-ops if ID not found

---

## 7. Storage Format & Security

Persistence is handled via flat-file storage using pipe delimiters (`|`). All user input is sanitized before writing — any `|` characters are escaped to `&#124;` to prevent data corruption, and unescaped on load. Corrupted lines are silently skipped and logged without crashing the application.

### `applications.dat`
```
id|companyName|roleTitle|pay|location|status|dateApplied|deadline|notes
```
Example:
```
uuid-1234|Google|SWE Intern|5000.0|Singapore|APPLIED|2026-03-01||Loves Python &#124; C++
```

### `interviews.dat`
```
id|applicationId|round|date|notes
```
Example:
```
uuid-5678|uuid-1234|1|2026-03-15T10:00|Very friendly interviewer
```

### `reminders.dat`
```
id|applicationId|type|triggerDate|dismissed
```
Example:
```
uuid-9999|uuid-1234|DEADLINE|2026-04-01|false
```

### Escaping Rules

| Character | Written As | Restored On Load |
|:---|:---|:---|
| `\|` (pipe) | `&#124;` (HTML entity) | `&#124;` → `\|` via `unescape()` |
| `null` string | `""` (empty string) | `escape(null)` returns `""` |

- **Escape** is applied to all user-editable string fields: `id`, `companyName`, `roleTitle`, `location`, `notes`, `applicationId`, and interview `notes`.
- **Not escaped**: numeric fields (`pay`, `round`), enum fields (`status`, `type`), date fields (`dateApplied`, `deadline`, `triggerDate`, `date`), and boolean fields (`dismissed`) — these are serialized via `.toString()` / `.name()` directly.

### Parsing Rules & Edge Cases

| Scenario | Behaviour |
|:---|:---|
| File does not exist | `readLines()` returns an empty list — treated as no data yet |
| File exists but is empty | Returns an empty list — no records |
| Line is `null` or blank | Skipped (`parseXxx()` returns `null`, filtered out) |
| Line has fewer fields than expected | Skipped (field count check: `< 9` for applications, `< 5` for interviews/reminders) |
| `pay` is not a valid double | `NumberFormatException` caught → line skipped, logged at `WARNING` |
| `status` is not a valid `ApplicationStatus` enum | `IllegalArgumentException` caught → line skipped, logged at `WARNING` |
| `dateApplied` / `deadline` / `date` is malformed | `DateTimeParseException` caught → line skipped, logged at `WARNING` |
| `deadline` field is empty string | Parsed as `null` (`LocalDate`) — deadline is optional |
| Duplicate application ID on save | `saveApplication()` checks for existing ID and skips the duplicate |
| Duplicate interview/reminder ID on save | **Not checked** — callers are responsible for not saving the same object twice |
| IOException on read | Logged at `SEVERE`, throws `RuntimeException` — GUI catches and shows error dialog |
| IOException on write | Logged at `SEVERE`, throws `RuntimeException` — GUI catches and shows error dialog |
| Data directory does not exist | Created automatically by `ensureDataDir()` on first write |
| Data directory cannot be created | Throws `RuntimeException` — GUI catches and shows error dialog |

### ID Generation

All entity IDs (`Application`, `Interview`, `Reminder`) are generated via `UUID.randomUUID().toString()` at construction time. IDs are immutable (`final`) after creation. The standard constructor (used for new records) generates the ID; the full constructor (used by `FileStorage` when loading from disk) accepts the existing ID.

### File I/O Strategy

- **Parsing**: `split("\\|", -1)` is used to split lines, preserving trailing empty fields.
- **Writing**: The entire file is rewritten on every save/update/delete operation (`Files.write(path, lines)`). This is a full-rewrite strategy, not append-only.
- **Concurrency**: No file locking is implemented — the app assumes single-user, single-instance access.

---

## 8. Error & Exception Handling

| Exception | Source Layer | Trigger Condition |
|:---|:---|:---|
| `IllegalArgumentException` | Logic | Null/blank Company Name or Role Title on `addApplication` or `updateDetails` |
| `IllegalArgumentException` | Logic | ID not found in `getApplicationById`, `updateStatus`, `updateDetails`, `updateDeadline`, `updateNotes`, `deleteApplication`, or `updateNotes` (InterviewController) |
| `IllegalArgumentException` | Logic | Parent application not found when calling `addInterview` or `addReminder` (referential integrity) |
| `IllegalStateException` | Logic | Status transition violation — modifying a `REJECTED`, `ACCEPTED`, or `WITHDRAWN` application, or jumping from `APPLIED` to `OFFER` |
| `RuntimeException` | Storage | Directory creation fails or I/O permissions block file access |
| `RuntimeException` | Storage | File read or write failure — logged via `java.util.logging.Logger` |
| *(Handled internally)* | Storage | Corrupt line in `.dat` file — logged at `WARNING` level and skipped, app continues running |
