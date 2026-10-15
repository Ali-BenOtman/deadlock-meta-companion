CREATE TABLE processed_files (
    file_key     TEXT        PRIMARY KEY,
    kind         TEXT        NOT NULL,
    rows         BIGINT      NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);