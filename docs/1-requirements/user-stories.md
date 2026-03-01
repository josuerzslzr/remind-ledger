# RemindLedger – User Stories

User stories for RemindLedger.  
Platform: **web first**, mobile later.  
Notifications: **multiple channels** (in-app + others TBD).

---

## Epic: Reminder management

**As an end user**, I want to register reminders by entering **name**, **recurrence/frequency**, and **duration (validity)** so that the tool reminds me at the configured time for daily activities (e.g. taking a med, tasks) as a busy modern person.

### US-1.1 – Create reminder

- **As an** end user  
- **I want to** create a reminder with name, recurrence/frequency, and duration (how long it is active, e.g. 30 days or until I delete it)  
- **So that** I get reminded at the right time for my activity.

**Acceptance criteria:**

- [ ] I can enter a reminder name.
- [ ] I can set recurrence/frequency (e.g. daily, weekly, custom).
- [ ] I can set duration/validity (e.g. “for 30 days” or “until I delete it”).
- [ ] I can set the time(s) when I want to be reminded.
- [ ] The reminder is saved and appears in my list.

*Details (e.g. one-time vs recurring, exact recurrence options) to be refined in design.*

---

## Epic: Notifications

**As an end user**, I want to be notified at the configured time so that I don’t miss the activity.

### US-2.1 – Receive reminder at configured time

- **As an** end user  
- **I want to** be notified at the configured time  
- **So that** I know I need to do the activity (e.g. take a med).

**Acceptance criteria:**

- [ ] I am notified when the reminder is due.
- [ ] Notifications are delivered via multiple channels (in-app + browser; exact channels TBD in design).

---

## Epic: Platform and quality (software engineer)

**As a software engineer**, I want to deliver a user-friendly but robust UX and architecture so that the system is maintainable and scalable.

### US-3.1 – User-friendly, robust architecture and UX

- **As a** software engineer  
- **I want** a user-friendly, robust architecture and UX using conventional enterprise level tooling and frameworks, cloud-native on AWS.  
- **So that** the system is maintainable, scalable, and pleasant to use.

**Acceptance criteria:**

- [ ] UX is clear and easy to use (web first).
- [ ] Architecture is cloud-native and AWS-hosted (details in design).
- [ ] Technology choices use frameworks of conventional use (to be specified in design).

---

## Summary

| Epic | Stories | Notes |
|------|---------|--------|
| Reminder management | US-1.1 Create reminder | Name, recurrence/frequency, duration (validity), time |
| Notifications | US-2.1 Receive at configured time | Multiple channels TBD |
| Platform and quality | US-3.1 UX and architecture | Web first; cloud-native, AWS |
