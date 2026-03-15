# Product Requirements Document (PRD)

## Internship / Job Tracker for Students

**Project Name:** Internship / Job Tracker
**Document Type:** Product Requirements Document
**Version:** 1.0
**Date:** 15 March 2026
**Based on:** Software Design Document v1.0

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Stakeholders](#2-stakeholders)
3. [User Stories](#3-user-stories)
4. [Feature List](#4-feature-list)
5. [Functional Requirements](#5-functional-requirements)
6. [Non-Functional Requirements](#6-non-functional-requirements)
7. [Use Cases](#7-use-cases)
8. [Constraints](#8-constraints)
9. [Glossary](#9-glossary)

---

## 1. Product Overview

### 1.1 Purpose

The Internship / Job Tracker is a centralized application that helps students manage all their job and internship applications in one place. Students applying to multiple roles simultaneously often lose track of application statuses, interview schedules, offer deadlines, and company notes. This product addresses that problem by providing structured tracking, reminders, sort and filter features, and a personal document vault.

### 1.2 Problem Statement

Students face the following pain points during their job search:

- Losing track of which stage they are at with each company
- Missing offer acceptance deadlines due to no centralized reminder system
- Repeating data entry (resume, personal details) across every application
- Struggling to arrange opportunities according to personal criteria
- Forgetting interview notes and company-specific research after conversations

### 1.3 Target Users

Students who are actively applying for internships, part-time jobs, or full-time jobs and are managing multiple applications at any given time.

---

## 2. Stakeholders

| Stakeholder | Role | Interest |
|---|---|---|
| Students | Primary User | Track applications, prepare for interviews, compare offers |
| Course Supervisor | Internal | Ensure product meets academic and design requirements |

---

## 3. User Stories

User stories are written in the format: **As a [role], I want to [goal], so that [benefit].**

All user stories are prioritized using three levels:

- **Level 0 — Essential:** Must be delivered for the product to be usable
- **Level 1 — Typical:** Expected by users and significantly improves experience

| ID | User Story | Priority |
|---|---|---|
| US-01 | As a student, I want to be able to monitor the deadline of offers, so that I can decide which ones to accept or decline. | Level 0 |
| US-02 | As a student, I want to filter and compare internships by pay, location, and job scope, so that I can prioritize which roles to focus on. | Level 0 |
| US-03 | As a student, I want to store and reuse my resume, documents, and personal details in one place, so that I don't have to repeatedly retype or re-upload the same information for every application. | Level 1 |
| US-04 | As a student, I want to track interview rounds so that I know which stage I am in. | Level 0 |
| US-05 | As a student, I want to store interview notes so that I can prepare better. | Level 1 |
| US-06 | As a student, I want to see upcoming interview dates in one place so that I can prepare accordingly. | Level 0 |
| US-07 | As a student, I want to keep a set of notes on the company itself and what was discussed during the interviews. | Level 1 |

---

## 4. Feature List

### Feature 1 — Application Tracking Dashboard

- Centralized dashboard showing all applications and their statuses
    - Track stages: Applied → Interview Rounds → Offer → Rejected / Accepted
    - View application progress at a glance

### Feature 2 — Deadline & Interview Reminders

- Notifications for:
    - Offer acceptance deadlines
    - Interview schedules
    - Follow-ups with HR

### Feature 3 — Calendar-Style Overview of Upcoming Events

- A calendar view displaying all upcoming events across all applications in one place

### Feature 4 — Internship Sorting and Filtering

- Compare internships by:
    - Salary
    - Location / travel time
    - Job scope

---

## 5. Functional Requirements

Functional requirements specify **what the system must do**.

### 5.1 Application Tracking Dashboard

| ID | Requirement | Linked User Story | Priority |
|---|---|---|---|
| FR-01 | The system shall allow students to create a new application entry with fields: Company Name, Role Title, Date Applied, and Status. | US-04 | Level 0 |
| FR-02 | The system shall allow students to update the status of any application to one of the following stages: Applied, Interview Round 1, Interview Round 2, Offer, Accepted, Rejected. | US-04 | Level 0 |
| FR-03 | The system shall display all application entries on a dashboard with their current status visible at a glance. | US-04 | Level 0 |
| FR-04 | The system shall allow students to delete an application entry. | US-04 | Level 0 |

### 5.2 Deadline & Interview Reminders

| ID | Requirement | Linked User Story | Priority |
|---|---|---|---|
| FR-05 | The system shall allow students to set an offer acceptance deadline date on any application marked as "Offer". | US-01 | Level 0 |
| FR-06 | The system shall display an in-app notification when an offer acceptance deadline is approaching. | US-01 | Level 0 |
| FR-07 | The system shall allow students to add interview dates and times to any application entry. | US-06 | Level 0 |
| FR-08 | The system shall display an in-app notification when an interview schedule is approaching. | US-06 | Level 0 |
| FR-09 | The system shall allow students to set a follow-up reminder with a note and target date for any application. | US-06 | Level 1 |
| FR-10 | The system shall display an in-app notification when a follow-up reminder date is reached. | US-06 | Level 1 |

### 5.3 Calendar-Style Overview of Upcoming Events

| ID | Requirement | Linked User Story | Priority |
|---|---|---|---|
| FR-11 | The system shall provide a calendar view displaying all upcoming interviews, offer deadlines, and follow-up reminders across all applications. | US-06 | Level 1 |
| FR-12 | The system shall allow students to select a date on the calendar to view all events occurring on that date. | US-06 | Level 1 |

### 5.4 Internship Comparison Tool

| ID    | Requirement | Linked User Story | Priority |
|-------|---|---|---|
| FR-13 | The system shall allow students to enter comparison fields for each application: salary, location, travel time, and job scope. | US-02 | Level 0 |
| FR-14 | The system shall allow students to filter applications by salary, location, or job scope before comparing. | US-02 | Level 1 |

### 5.5 Notes & Document Storage

| ID    | Requirement | Linked User Story | Priority |
|-------|---|---|---|
| FR-15 | The system shall allow students to upload and store personal documents (e.g., resume, cover letter, transcript) locally. | US-03 | Level 1 |
| FR-16 | The system shall allow students to store personal details (e.g., full name, email, phone number) for quick reference. | US-03 | Level 1 |
| FR-17 | The system shall allow students to write and save free-text interview notes per application. | US-05 | Level 1 |
| FR-18 | The system shall allow students to write and save a company research notes section per application. | US-07 | Level 1 |

---

## 6. Non-Functional Requirements

Non-functional requirements specify **the constraints and quality standards** the system must meet.

### 6.1 Performance

| ID | Requirement |
|---|---|
| NFR-01 | The system shall load the main dashboard within 2 seconds under normal usage. |
| NFR-02 | Search and filter operations shall return results within 1 second for up to 200 application entries. |

### 6.2 Usability

| ID | Requirement |
|---|---|
| NFR-03 | The system shall provide a clear and intuitive interface that a first-time user can navigate without a manual. |
| NFR-04 | Updating an application status shall be achievable in no more than 2 user interactions. |

### 6.3 Security & Privacy

| ID     | Requirement |
|--------|---|
| NFR-05 | All data shall be stored locally on the user's device; no data shall be transmitted to any external server. |
| NFR-06 | Uploaded documents shall be accessible only through the application. |

### 6.4 Portability

| ID     | Requirement                                                     |
|--------|-----------------------------------------------------------------|
| NFR-07 | The system shall operate fully without an internet connection.  |
| NFR-08 | The system shall not depend on any third-party API or database. |

---

## 7. Use Cases

### UC-01: Track Interview Round Progress

**Actor:** Student

**Precondition:** At least one application entry exists in the system.

**Main Flow:**
1. Student opens the Application Dashboard.
2. Student selects an application entry.
3. Student updates the status to the current interview round (e.g., Interview Round 1).
4. System saves and reflects the updated status on the Dashboard.

**Postcondition:** The application status is updated and visible on the Dashboard.

---

### UC-02: Monitor Offer Deadline

**Actor:** Student

**Precondition:** An application entry exists with its status set to "Offer".

**Main Flow:**
1. Student opens the application entry.
2. Student enters the offer acceptance deadline date.
3. System saves the deadline.
4. When the deadline is approaching, the system displays an in-app notification.

**Postcondition:** The deadline is saved and an in-app notification appears when it is approaching.

---

### UC-03: Compare Internships

**Actor:** Student

**Precondition:** At least two application entries exist with comparison fields (salary, location, job scope) populated.

**Main Flow:**
1. Student navigates to the Internship Comparison Tool.
2. Student selects two or more applications to compare.
3. System displays a side-by-side view showing salary, location / travel time, and job scope.
4. Student optionally filters the list before selecting applications to compare.

**Postcondition:** Student can view a structured comparison of selected opportunities.

---

### UC-04: Log Interview Notes

**Actor:** Student

**Precondition:** An application entry exists with at least one interview round recorded.

**Main Flow:**
1. Student opens an application entry.
2. Student navigates to the Notes section.
3. Student writes general interview notes in the free-text field.
4. System saves all notes.

**Postcondition:** Notes are saved and retrievable at any time.

---

## 8. Constraints

| Constraint | Description |
|---|---|
| No external services | The system must not call any third-party APIs or external services. |
| No internet dependency | All features must function fully offline. |
| Internal data handling | All data (applications, documents, notes) must be stored and managed locally on the user's device. |
| 3-layer architecture | Development must follow the UI / Logic / Storage separation as defined in the SDD. |
| Team split | The codebase is divided among three workstreams: UI, Logic, and Storage (Database). |

---

## 9. Glossary

| Term | Definition |
|---|---|
| Application Entry | A record representing a single job or internship application, containing all associated data such as company name, status, dates, notes, and documents. |
| Application Status | The current stage of an application in the hiring pipeline: Applied, Interview Round 1, Interview Round 2, Offer, Accepted, or Rejected. |
| Document Vault | A local storage area within the application where students can upload and manage personal documents such as resumes and cover letters. |
| Offer Deadline | The date by which a student must accept or reject a job or internship offer from a company. |
| Reminder | An in-app notification triggered by a date threshold, alerting the student to an upcoming event such as an interview, offer deadline, or follow-up. |
