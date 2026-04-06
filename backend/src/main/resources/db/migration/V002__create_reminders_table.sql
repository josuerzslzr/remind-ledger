CREATE TABLE reminders (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES users(id),
    name           VARCHAR(255) NOT NULL,
    schedule_type  VARCHAR(20)  NOT NULL,
    times          TIME[]       NOT NULL,
    date           DATE,
    days_of_week   TEXT[],
    day_of_month   INTEGER,
    channels       TEXT[]       NOT NULL,
    valid_until    DATE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_schedule_type
        CHECK (schedule_type IN ('ONCE', 'DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_day_of_month
        CHECK (day_of_month IS NULL OR day_of_month BETWEEN 1 AND 31)
);

CREATE INDEX idx_reminders_user_id ON reminders(user_id);
