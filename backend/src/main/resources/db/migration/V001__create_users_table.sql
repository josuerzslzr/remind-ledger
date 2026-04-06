CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cognito_sub  VARCHAR(255) NOT NULL,
    email        VARCHAR(255),
    display_name VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_cognito_sub UNIQUE (cognito_sub)
);
