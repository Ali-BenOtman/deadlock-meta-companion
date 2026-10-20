# Data notes



## Source

Community Deadlock API data lake (not affiliated with Valve).

Hourly Parquet exports: https://deadlock-api.com/data-dumps



## Main table: match_player

- One row per player per match. \~361M rows, \~486 GB, split into many files.

- Never query the whole table; read single files with read_parquet().

- File names show build dates, not match dates. Low part numbers hold the oldest matches.

- "residual" files mix late additions from many dates.

- A row can appear twice (base + newer file): keep the newest per (match_id, account_id).



## Columns we use

- match_id, start_time, duration_s

- match_mode: use 'Ranked' only for the MVP

- average_badge: match's average rank (filled for recent ranked matches; empty in 2024 data)

- hero_id, team, won

- items.item_id, items.game_time_s, items.sold_time_s: parallel lists, same position = same item.

&#x20; sold_time_s = 0 means never sold. List may include ability upgrades, not just shop items.



## Open questions

- Lookup lists for hero names, item names and badge values

- How to tell shop items from abilities in the items list

- Which files hold the bulk of recent ranked matches


## Findings from the API docs
- Lookup lists: /v1/assets/heroes, /v1/assets/items, /v1/assets/ranks on api.deadlock-api.com
- Badge = tier (first digits) + subtier (last digit), max 116
- Ranks exist only from the first ranked season (2026-07-30)
- Analytics endpoints are rate-limited (200 req/min per IP); fine for cached lookups
- Corrupted items (build 6712+, from 2026-09-29) share the normal item's id: must detect and exclude
- Filter to game_mode normal (exclude street_brawl)
- Raw item win rates are confounded by wealth (ahead players buy sooner): known limitation for the MVP

## Findings from the first Java load
- Daily residual files contain few ranked matches (~100 in a 37.6K-row file)
- Not all ranked rows have average_badge (70% in one file): skip null badges in rank-split stats

## Assets (lookup lists)
- Loaded at startup from /v1/assets/heroes, /items, /ranks and kept in memory
- Item types: ability (418), upgrade (250), weapon (89). Shop items = type "upgrade"
- Item ids exceed int range: store as long
- Hero list (65) includes non-playable heroes: filter on player_selectable and disabled
- Rank name = ranks[badge / 10] + " " + (badge % 10), e.g. 13 = Initiate 3
- If the assets API is down at startup, the app still starts; names show as "Unknown ..."

## Ingestion findings
- game_mode values: Normal, StreetBrawl (case-sensitive)
- account_id = 0 only appears for bots (CoopBot matches); filtered out with account_id <> 0
- Ability upgrades can repeat with identical (item, time): filtered by isShopItem, and ON CONFLICT DO NOTHING guards the primary key
- First sample load: 100 matches, 1,200 players, 21,122 shop item purchases; second run inserted 0 (idempotent)
- Data-quality check: every ingested match has exactly 12 players

## Folder ingestion
- Base file part-114 (newest base partition, ~11 hours of matches): 341K rows, 519 MB, ~25% ranked; all ranked rows have average_badge
- Loaded totals after two files: 6,570 ranked matches, 78,787 players, ~1.32M shop item purchases
- Re-processing an already-loaded file inserts 0 rows (idempotent across files)
- 53 matches (~0.8%) have 11 players instead of 12: missing in the source data, not filtered by us
- Base files are rebuilt as new matches arrive, so file names change over time

## Ingestion speed baseline (before optimization)
- extract_p116_p116.parquet: 14,278 matches, 171,261 players, 2,850,117 item purchases
- 544 seconds (9 min 4 s), about 5,500 rows/second, using JdbcTemplate batchUpdate with batches of 50,000
- The extract contained each player row about twice (base + delta copies); ingestion deduplication kept the newest

## Full backfill (partitions 106-116)
- Extraction (106-114): 514 source files, ~84 GB scanned, ~568 MB of local extracts, ~18 minutes total
- Ingestion (106-114): 4 h 13 min, steady ~5,500 rows/s; partition 114 skipped the 77,586 rows already loaded from part-114
- Totals: 423,486 matches, 5,079,886 players, 87,006,417 item purchases, 13 GB database
- Coverage: every day from 2026-09-25 to 2026-10-14 (14-18k ranked matches per day; 14 Oct partial)
- 2026-09-29 has a dip (11,257 matches), the date of build 6712: a likely patch-day effect

## Stats endpoint speed before precomputation (87M item rows)
- /api/meta: 4.5 s first request, 3.6 s second
- /api/heroes/84/items: 32.5 s first (cold), 4.2 s second (warm cache)
- /api/heroes/84/builds: failed both times at the 300 s socket timeout
- Conclusion: computing stats on every request does not scale; precompute into summary tables

## Stats endpoint speed after precomputation
- Refresh: 521 s (complete matches 1 s, hero stats 1 s, item stats 368 s, build stats 151 s)
- /api/meta 0.007 s, /api/heroes/84/items 0.009 s, /api/heroes/84/builds 0.108 s
- Larger samples exposed noise: the Intensifying Magazine Rat King build went from +5.7 (411 games) to +0.6 (2,318 games)

## First automatic update (2026-10-16)
- 65 delta/residual files (7.9 GB source) -> 612,696 rows extracted in 144 s; ingestion ~33 min; refresh 9.2 min; total 44 min
- Added 22,776 matches, 273,225 players, 4,587,472 item purchases; complete matches now 444,270
- Immediate rerun: NO_NEW_FILES in 0.4 s. First scheduled run at startup + 1 min: NO_NEW_FILES

## Production server (Contabo VPS, 2026-10-19/20)
- Hardware: 4 shared vCPU, 8 GB RAM, 100 GB SSD, Ubuntu 24.04, Docker Compose with Caddy in front
- First scheduled update on an empty database: 60 delta/residual files (9.6 GB source) -> 580,214 players,
  9.9M item purchases; extraction 43 s, ingestion ~7.5 min, total 561.7 s
- Ingestion is ~3-4x faster than on the Windows laptop: no Docker Desktop VM networking layer between
  the app and PostgreSQL. The ~5,500 rows/s measured locally was largely a Windows/Docker Desktop effect.
- Backfill of partitions 108-120: ~1 min extraction per partition (~12 GB source -> ~70 MB each);
  ingestion of 13 files: 329,941 matches, 3.96M players, 67.4M item purchases.
  Partition 121 did not exist yet (HTTP 500 for that call, expected).
- Stats refresh on the full 14-day window (~243k matches): 1,291-1,370 s on the server vs 333 s on the laptop.
  Item stats alone: 930 s. CPU-heavy aggregation is ~4x slower on shared vCPUs -> main optimization target.
- Typical scheduled update (3 new files, ~3.4k matches): 1,558 s total, almost all of it the stats refresh.
- External checks: SSL Labs A+, securityheaders.com A+, only ports 22/80/443 reachable from outside.
- Within ~20 s of going live, an automated scanner probed for a Jira vulnerability (META-INF path);
  Spring rejected it.

  ## Faster stats refresh (2026-10-20, laptop, ~242k matches in window)
- Before: item stats 231.6 s, build stats 93.9 s, total 328.0 s
- After: window players 1.0 s (2.91M rows), window first buys 61.2 s (48.98M rows),
  item stats 54.8 s, build stats 41.8 s, total 163.6 s -> 2.0x faster
- Identical output row counts (473 hero, 68,882 item, 916,510 build rows);
  integration tests (exact expected values) unchanged and green