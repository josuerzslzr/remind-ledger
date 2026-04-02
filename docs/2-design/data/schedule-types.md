# Schedule Types — Column Usage by Type

Each Reminder has a `schedule_type` that determines which schedule-related columns
are required, optional, or unused.

**Types:** `once`, `daily`, `weekly`, `monthly`

> **Note on "every N hours" reminders:** There is no `interval` schedule type.
> The frontend offers an "every N hours" helper that expands the interval into
> concrete times (e.g. "every 6 hours" → `[00:00, 06:00, 12:00, 18:00]`).
> The user can then review, add, or remove individual times before saving.
> What gets stored is a `daily` reminder with the final `times` array.
> On the edit screen the user sees the concrete times, not "every N hours."

## Column matrix

| Column          | `once`                 | `daily` | `weekly` | `monthly` |
| --------------- | ---------------------- | ------- | -------- | --------- |
| `schedule_type` | **req**                | **req** | **req**  | **req**   |
| `times`         | **req** (single entry) | **req** | **req**  | **req**   |
| `date`          | **req**                | —       | —        | —         |
| `days_of_week`  | —                      | —       | **req**  | —         |
| `day_of_month`  | —                      | —       | —        | **req**   |
| `channels`      | **req**                | **req** | **req**  | **req**   |
| `valid_until`   | —                      | opt     | opt      | opt       |

**req** = required, **opt** = optional, **—** = not used (must be null)

## Examples per type

### `once` — Single fire

> "Remind me to call the dentist on April 15 at 3 PM"

| Column          | Value        |
| --------------- | ------------ |
| `schedule_type` | `once`       |
| `date`          | `2026-04-15` |
| `times`         | `[15:00]`    |
| `channels`      | `[web_push]` |

EventBridge (one-time schedule): `at(2026-04-15T15:00:00)`

### `daily` — Every day at fixed time(s)

> "Take vitamins at 9 AM and 9 PM"

| Column          | Value               |
| --------------- | ------------------- |
| `schedule_type` | `daily`             |
| `times`         | `[09:00, 21:00]`    |
| `channels`      | `[web_push, email]` |

Cron: `cron(0 9,21 * * ? *)`

> "Take antibiotic every 6 hours for 7 days"
>
> User selects "every 6 hours" in the UI → frontend expands to
> `[00:00, 06:00, 12:00, 18:00]` → user reviews and adjusts →
> saved as `daily`.

| Column          | Value                              |
| --------------- | ---------------------------------- |
| `schedule_type` | `daily`                            |
| `times`         | `[00:00, 06:00, 12:00, 18:00]`    |
| `valid_until`   | `2026-03-18`                       |
| `channels`      | `[web_push, sms]`                  |

Cron: `cron(0 0,6,12,18 * * ? *)`

> "Drink water every 2 hours during the day"
>
> User selects "every 2 hours" from 8 AM to 8 PM → frontend expands →
> user keeps all times → saved as `daily`.

| Column          | Value                                          |
| --------------- | ---------------------------------------------- |
| `schedule_type` | `daily`                                        |
| `times`         | `[08:00, 10:00, 12:00, 14:00, 16:00, 18:00, 20:00]` |
| `channels`      | `[in_app]`                                     |

Cron: `cron(0 8,10,12,14,16,18,20 * * ? *)`

> "Take pills at 9:00 AM, 2:30 PM, and 9:00 PM"
>
> Minutes differ across entries (`:00` and `:30`), so one cron per
> minute group.

| Column          | Value                        |
| --------------- | ---------------------------- |
| `schedule_type` | `daily`                      |
| `times`         | `[09:00, 14:30, 21:00]`     |
| `channels`      | `[web_push]`                 |

Cron (grouped by minute):
- `cron(0 9,21 * * ? *)` — the `:00` times
- `cron(30 14 * * ? *)` — the `:30` times

### `weekly` — Specific days at fixed time(s)

> "Team standup Mon/Wed/Fri at 10 AM"

| Column          | Value              |
| --------------- | ------------------ |
| `schedule_type` | `weekly`           |
| `days_of_week`  | `[MON, WED, FRI]`  |
| `times`         | `[10:00]`          |
| `channels`      | `[web_push]`       |

Cron: `cron(0 10 ? * MON,WED,FRI *)`

### `monthly` — Specific day of month at fixed time(s)

> "Pay rent on the 1st at 9 AM"

| Column          | Value               |
| --------------- | ------------------- |
| `schedule_type` | `monthly`           |
| `day_of_month`  | `1`                 |
| `times`         | `[09:00]`           |
| `channels`      | `[web_push, email]` |

Cron: `cron(0 9 1 * ? *)`

## Cron construction rules

The backend transforms `times` into EventBridge cron expressions using these rules:

1. **`once`** — use EventBridge one-time syntax: `at(YYYY-MM-DDThh:mm:00)`.
2. **All other types** — group times by minute value:
   - If all times share the same minute (common case, e.g. all on `:00`),
     produce a **single cron** with comma-separated hours.
     `[09:00, 21:00]` → `cron(0 9,21 * * ? *)`
   - If minutes differ, produce **one cron per minute group**.
     `[09:00, 14:30, 21:00]` → `cron(0 9,21 * * ? *)` + `cron(30 14 * * ? *)`
3. **`weekly`** — add `days_of_week` to the day-of-week field.
   `cron(0 10 ? * MON,WED,FRI *)`
4. **`monthly`** — add `day_of_month` to the day-of-month field.
   `cron(0 9 1 * ? *)`

Each resulting cron expression maps to one EventBridge Scheduler schedule.

## Notes

- `valid_until` is type-independent: any recurring type (daily, weekly, monthly)
  can optionally have an end date. If null, the reminder runs until deleted.
- `once` reminders ignore `valid_until` — they fire once and are done.
- `times` is always an array, even for `once` (single entry). This keeps the
  backend handling consistent across types.
- The "every N hours" UI helper is a frontend-only concept. The backend receives
  and stores concrete times — it has no knowledge of interval logic.

