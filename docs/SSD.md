# Software Design Document (SDD)
## Internship / Job Tracker for Students

**Project Name:** Internship / Job Tracker  
**Document Type:** Software Design Document  
**Version:** 1.0  

---

# 1. System Overview

## 1.1 Purpose
The purpose of this system is to help students manage and track their internship and job applications in one centralized platform. Many students apply to multiple companies at once and often struggle to keep track of deadlines, interview schedules, follow-ups, and offer decisions. This system provides a structured way to organize all applications and compare opportunities.

## 1.2 Scope
The system will support the following core features:

- **Application Tracking Dashboard**
  - Centralized dashboard showing all applications and their statuses
  - Track stages such as:
    - Applied
    - Interview Round 1
    - Interview Round 2
    - Offer
    - Rejected
    - Accepted
  - View overall application progress at a glance

- **Deadline & Interview Reminders**
  - Reminders for:
    - Offer acceptance deadlines
    - Interview schedules
    - Follow-ups with HR
  - Calendar-style overview of upcoming events

- **Internship Comparison Tool**
  - Compare opportunities by:
    - Salary
    - Location / travel time
    - Job scope

## 1.3 Intended Users
The intended users are students who are applying for internships, part-time jobs, or full-time jobs and need a convenient way to manage multiple applications.

## 1.4 Constraints
- The system must **not use any external services**
- No calls to third-party APIs are allowed
- All data must be handled internally by the system
- The work is split into:
  - **UI**
  - **Logic**
  - **Storage (Database)**

---

# 2. Architecture Design

## 2.1 Architectural Style
The system will follow a **3-layer architecture**:

1. **Presentation Layer (UI)**
   - Responsible for displaying data to the user
   - Handles forms, dashboard views, comparison page, and calendar/reminder views

2. **Business Logic Layer**
   - Responsible for processing application data
   - Applies rules for status updates, comparisons, reminders, and filtering

3. **Data Layer (Storage / Database)**
   - Responsible for storing and retrieving all application, reminder, and comparison data

This architecture is chosen because it separates responsibilities clearly and supports team collaboration.

## 2.2 High-Level Architecture Diagram

flowchart TD
    A[User] --> B[UI Layer]
    B --> C[Logic Layer]
    C --> D[Database Layer]
    D --> C
    C --> B