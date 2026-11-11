-- Item stats split by game phase: 1 = first bought before 10:00, 2 = from 10:00 to 19:59, 3 = from 20:00.
-- Rebuilt by every stats refresh, like the other summary tables.
CREATE TABLE item_phase_stats (
    hero_id SMALLINT NOT NULL,
    item_id BIGINT   NOT NULL,
    tier    SMALLINT NOT NULL,
    phase   SMALLINT NOT NULL,
    buyers  INTEGER  NOT NULL,
    wins    INTEGER  NOT NULL,
    PRIMARY KEY (hero_id, item_id, tier, phase)
);