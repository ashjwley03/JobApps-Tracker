# Software Design Document (SDD)
## Internship / Job Tracker for Students

**Project Name:** Internship / Job Tracker  
**Document Type:** Software Design Document  
**Version:** 2.0  
**Date:** 16 April 2026  

---

# 1. System Overview

## 1.1 Purpose
The purpose of this system is to help students manage and track their internship and job applications in one centralized platform. Many students apply to multiple companies at once and often struggle to keep track of deadlines, interview schedules, follow-ups, and offer decisions. This system provides a structured way to organize all applications and compare opportunities.

## 1.2 Scope
The system supports the following core features:

- **Application Tracking Dashboard**
  - Centralized dashboard showing all applications and their statuses
  - Track stages:
    - Applied
    - Interviewing
    - Offer
    - Rejected
    - Accepted
    - Withdrawn
  - View overall application progress at a glance via stat cards, bar chart, and pie chart
  - Search/filter applications by company name or role title

- **Application Editing**
  - Edit core details: company name, role title, pay, location
  - Update status with enforced transition rules (terminal states: Rejected, Accepted, Withdrawn)
  - Set or clear deadline dates
  - Add or update free-text notes
  - Delete an application with confirmation

- **Deadline & Interview Reminders**
  - Reminders for:
    - Offer acceptance deadlines
    - Interview schedules
    - Follow-ups with HR
  - Calendar-style overview of upcoming events with colour-coded badges

- **Internship Comparison Tool**
  - Compare opportunities by selecting multiple applications from a list
  - Side-by-side comparison showing:
    - Company, Role, Pay, Location, Status, Deadline
  - Best-pay row is visually highlighted

## 1.3 Intended Users
The intended users are students who are applying for internships, part-time jobs, or full-time jobs and need a convenient way to manage multiple applications.

## 1.4 Constraints
- The system must **not use any external services**
- No calls to third-party APIs are allowed
- All data must be handled internally by the system via local flat-file storage
- The work is split into:
  - **UI** (GUI Layer)
  - **Logic** (Business Logic Layer)
  - **Storage** (Data Layer)

---

# 2. Architecture Design

## 2.1 Architectural Style
The system follows a **3-layer architecture**:

1. **Presentation Layer (GUI)**
   - Responsible for displaying data to the user
   - Handles the dashboard, edit form, new application form, comparison page, and calendar view
   - Controllers: MainController, DashboardController, EditApplicationController, NewApplicationController, CompareController, CalendarController, GuiUtils

2. **Business Logic Layer**
   - Responsible for processing application data
   - Applies rules for status updates (including terminal state enforcement), comparisons, reminders, and filtering
   - Controllers: ApplicationController, InterviewController, ReminderService

3. **Data Layer (Storage)**
   - Responsible for storing and retrieving all application, interview, and reminder data
   - Uses pipe-delimited flat files (`.dat`) with escape/unescape for special characters
   - Components: Storage (interface), FileStorage, InMemoryStorage (test stub)

This architecture is chosen because it separates responsibilities clearly and supports team collaboration.

## 2.2 High-Level Architecture Diagram

**Source:** `SystemArchitecture.puml`

![System Architecture Diagram](System%20Architecture%20Diagram.png)

## 2.3 GUI Layer Components

| Controller | Responsibility |
|---|---|
| MainController | Root controller; manages navigation between views and injects shared dependencies |
| DashboardController | Displays all applications in a table with stat cards, charts, search, and Edit buttons |
| EditApplicationController | Full edit form for an application: details, status, deadline, notes, delete |
| NewApplicationController | Form for creating a new application with input validation |
| CompareController | Multi-select list + comparison table sorted by pay descending |
| CalendarController | Monthly calendar grid with colour-coded event badges for deadlines, interviews, and reminders |
| GuiUtils | Utility class for displaying error dialogs |

## 2.4 Logic Layer Components

| Controller | Responsibility |
|---|---|
| ApplicationController | CRUD operations for applications, status transition enforcement, comparison, filtering |
| InterviewController | CRUD for interview records with referential integrity checks |
| ReminderService | CRUD for reminders with upcoming/dismissed filtering |

## 2.5 Storage Layer Components

| Component | Responsibility |
|---|---|
| Storage (interface) | Defines the contract for all persistence operations |
| FileStorage | Pipe-delimited flat-file implementation with escape/unescape and corrupt-line resilience |
| InMemoryStorage | In-memory test stub for unit testing without file I/O |

## 2.6 Data Model

| Entity | Key Fields |
|---|---|
| Application | id, companyName, roleTitle, pay, location, status, dateApplied, deadline, notes |
| Interview | id, applicationId, round, date, notes |
| Reminder | id, applicationId, type, triggerDate, dismissed |
| ApplicationStatus (enum) | APPLIED, INTERVIEWING, OFFER, REJECTED, ACCEPTED, WITHDRAWN |
| ReminderType (enum) | DEADLINE, INTERVIEW, FOLLOWUP |
