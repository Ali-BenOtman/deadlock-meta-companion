-- Visitor feedback: a message and an optional 1-5 rating. No address or other personal data is stored.
CREATE TABLE feedback (
    id         BIGSERIAL   PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    rating     SMALLINT CHECK (rating BETWEEN 1 AND 5),
    message    TEXT        NOT NULL CHECK (char_length(message) BETWEEN 1 AND 1000)
);