CREATE TABLE tier_match_totals (
    tier    SMALLINT PRIMARY KEY,
    matches INTEGER  NOT NULL
);

CREATE TABLE hero_tier_stats (
    hero_id SMALLINT NOT NULL,
    tier    SMALLINT NOT NULL,
    games   INTEGER  NOT NULL,
    wins    INTEGER  NOT NULL,
    PRIMARY KEY (hero_id, tier)
);

CREATE TABLE item_tier_stats (
    hero_id      SMALLINT NOT NULL,
    item_id      BIGINT   NOT NULL,
    tier         SMALLINT NOT NULL,
    buyers       INTEGER  NOT NULL,
    wins         INTEGER  NOT NULL,
    median_buy_s INTEGER  NOT NULL,
    PRIMARY KEY (hero_id, item_id, tier)
);

CREATE TABLE build_tier_stats (
    hero_id   SMALLINT NOT NULL,
    tier      SMALLINT NOT NULL,
    build_key TEXT     NOT NULL,
    build     BIGINT[] NOT NULL,
    games     INTEGER  NOT NULL,
    wins      INTEGER  NOT NULL,
    PRIMARY KEY (hero_id, tier, build_key)
);

CREATE TABLE stats_refresh (
    id           SMALLINT    PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    refreshed_at TIMESTAMPTZ NOT NULL,
    duration_ms  BIGINT      NOT NULL,
    matches_used INTEGER     NOT NULL
);