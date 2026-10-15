CREATE TABLE hero_daily_stats (
    hero_id SMALLINT NOT NULL,
    tier    SMALLINT NOT NULL,
    day     DATE     NOT NULL,
    games   INTEGER  NOT NULL,
    wins    INTEGER  NOT NULL,
    PRIMARY KEY (hero_id, tier, day)
);

ALTER TABLE stats_refresh
    ADD COLUMN window_start TIMESTAMPTZ,
    ADD COLUMN window_end   TIMESTAMPTZ;