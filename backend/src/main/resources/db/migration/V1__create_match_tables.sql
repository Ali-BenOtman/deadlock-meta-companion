CREATE TABLE matches (
    match_id      BIGINT      PRIMARY KEY,
    start_time    TIMESTAMPTZ NOT NULL,
    duration_s    INTEGER     NOT NULL,
    match_mode    TEXT        NOT NULL,
    average_badge SMALLINT,
    winning_team  TEXT,
    ingested_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE match_players (
    match_id   BIGINT   NOT NULL REFERENCES matches (match_id),
    account_id BIGINT   NOT NULL,
    hero_id    SMALLINT NOT NULL,
    team       TEXT     NOT NULL,
    won        BOOLEAN  NOT NULL,
    PRIMARY KEY (match_id, account_id)
);

CREATE TABLE item_purchases (
    match_id    BIGINT  NOT NULL,
    account_id  BIGINT  NOT NULL,
    item_id     BIGINT  NOT NULL,
    bought_at_s INTEGER NOT NULL,
    sold_at_s   INTEGER,
    PRIMARY KEY (match_id, account_id, item_id, bought_at_s),
    FOREIGN KEY (match_id, account_id) REFERENCES match_players (match_id, account_id)
);

CREATE INDEX idx_matches_start_time ON matches (start_time);
CREATE INDEX idx_match_players_hero ON match_players (hero_id);
CREATE INDEX idx_item_purchases_item ON item_purchases (item_id);