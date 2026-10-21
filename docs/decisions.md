# Decisions



## 1. Use the data lake, not the REST API, for bulk stats

The REST API is rate-limited; the data lake gives bulk Parquet files with no credentials.

We read only needed files and columns, so the 486 GB table is never downloaded in full.

## 2. Plain SQL with JdbcTemplate instead of JPA
Ingestion inserts thousands of rows at once and the stats are aggregate SQL queries.
Writing the SQL directly is faster for bulk loads and keeps full control over the queries.

## 3. Schema managed by Flyway migrations
Tables are created by versioned SQL files (V1, V2, ...) applied automatically at startup.
Schema changes are never made by hand, so every environment ends up identical.

## 4. Duplicates prevented by primary keys
matches: match_id. match_players: (match_id, account_id).
The source data can contain the same row twice; the database itself rejects the second copy.

## 5. account_id stored only as part of the key
It is a Steam account number (personal data). It is used to prevent duplicates and is never shown on the website.

## 6. Database password kept in infra/.env
Read by both Docker Compose and Spring Boot, never committed. infra/.env.example documents the required setting.

## 7. Idempotent ingestion
Inserts use ON CONFLICT DO NOTHING on each table's primary key, inside one transaction.
Re-running the job inserts nothing new, and a failure halfway rolls everything back.

## 8. Duplicate source rows resolved in DuckDB
A window function (ROW_NUMBER over match_id, account_id ordered by created_at DESC) keeps only the newest copy of each player row before anything reaches PostgreSQL.

## 9. One transaction per file
DirectoryIngestionService calls IngestionService.ingestFile for each file. Because the call crosses classes,
Spring's @Transactional applies per file: a failing file rolls back alone, earlier files stay saved.
(Calling ingestFile from inside IngestionService itself would bypass the transaction proxy.)

## 10. Incomplete matches are stored but excluded from stats
Ingestion keeps the source data as-is. Stats queries only use matches with all 12 players.

## Future improvement: remote extraction
Instead of downloading whole files, DuckDB could read only ranked rows and needed columns directly from the
data lake URLs. Better for automatic daily updates; not needed for the MVP.

## 11. Stats queries use bound parameters
User input (minTier) is passed as a ? parameter to JdbcTemplate, never inserted into the SQL text, to prevent SQL injection.
Values outside 0-11 are rejected with 400 Bad Request.

## 12. Rank stats only use matches with a known rank
Hero stats require average_badge (matches without one are excluded) and all 12 players present.

## 13. Item stats count each player once per item
A player can buy, sell and rebuy the same item. Purchases are grouped per player and item (keeping the first buy time)
so nobody's win or loss is counted twice.

## 14. Items ranked by win rate delta, not raw win rate
Delta = item win rate minus the hero's own win rate. A raw 57% means little on a hero who already wins 56%.
Items need at least 100 buyers. Buy time is the median, which is robust to outliers.

## Known limitation: item win rates are correlational
Late items look strong because players who reach them are already ahead (wealth effect).
Defensive and reactive items look weak because players often buy them when losing.
Corrupted items (Broker, since 2026-09-29) share their normal item id and are not yet separated.

## 15. Builds are starting builds: the first six shop items
Each player's first six distinct shop items (by first purchase time, item id as tie-breaker), order ignored
by sorting the array. These are mostly early laning items, so the feature is labelled "starting build".
Full end-game inventories almost never repeat exactly, so they cannot be counted reliably.
Builds need 30+ games; the 10 most popular are returned, with win rate delta versus the hero's baseline.

## 16. Smoothed win rates and tiers
smoothed = (wins + 200 x 50%) / (games + 200). The prior is exactly 50% because every match has 6 winners and 6 losers.
Tiers by smoothed distance from 50%: S >= +3, A +1 to +3, B between -1 and +1, C -1 to -3, D <= -3.
Heroes under 500 games get NOT_ENOUGH_DATA and are listed after all tiered heroes.
The calculation lives in MetaCalculator (no database, no Spring) and is covered by unit tests.

## Known limitation: sample size in high-rank brackets
At tier 8+, 17 of 41 heroes have under 500 games with the current data. Loading more days of matches fixes this.

## 17. Remote extraction through the data lake manifest
DuckDB's DuckLake catalog builds file paths with backslashes on Windows, which breaks HTTPS addresses.
Instead, the manifest is read with RestClient, files are selected by partition (match_id / 1,000,000),
and DuckDB reads the HTTPS URLs directly with read_parquet. Only 15 of 157 columns are fetched,
and rows are deduplicated before writing the extract: one partition is about 40 MB instead of several GB.

## 18. Item inserts are streamed in batches, with a socket timeout
Items are inserted in batches of 10,000 so memory stays flat. reWriteBatchedInserts caused a hang:
pg_stat_activity showed PostgreSQL waiting on the client (ClientRead) while a thread dump showed Java
blocked writing to the socket, with no data flowing. The setting was removed, and socketTimeout=300
makes any future stall fail with an error instead of hanging forever.

## Measured ingestion speed
About 5,600 rows/second with batched inserts (partition 115: 5.68M rows in 1,007 s).
PostgreSQL COPY is the next step if loading needs to be faster.

## 19. Stats are precomputed into summary tables
Computing stats per request stopped scaling at 87M item rows (meta 3.6 s, items 4-32 s, builds timed out after 300 s).
A refresh job (POST /api/dev/refresh-stats) computes everything inside PostgreSQL in one transaction (~9 minutes)
into small tables keyed by hero and exact rank tier. Endpoints add up the tiers at or above minTier:
counts add exactly, and win rates are recomputed from summed counts, never by averaging percentages.
Median buy time is approximated by a weighted average of per-tier medians.
DELETE (not TRUNCATE) inside one transaction keeps old stats readable until the new ones commit.
Result: meta 0.007 s, items 0.009 s, builds 0.108 s.

## 20. Redis cache on top of the summary tables
Stats responses are cached in Redis (@Cacheable, keyed by cache name and arguments, 1-hour TTL as a safety net).
The cache is cleared only after a refresh has committed (StatsCache.evictAll called after refresh() returns),
avoiding the race where a request recomputes from old data and re-caches stale numbers.
Records implement Serializable (Java serialization); JSON would be more readable and robust to class changes.
Verified: with PostgreSQL stopped, cached endpoints still answered, uncached ones failed.
Precomputation was the big speed win; Redis mainly protects the database under load.

## 21. Scheduled updates from delta files, tracked in processed_files
Every new row appears first in an hourly delta file, so the job only processes delta and residual files it has
not seen before (tracked by file key in processed_files), never the large rebuilt base files.
Steps: extract recent ranked matches (last 21 days) from new files -> ingest -> record files -> refresh stats
(only if rows were inserted) -> clear the cache -> delete the temporary extract.
Files are recorded only after a successful ingest: a crash means reprocessing, which idempotent ingestion makes
harmless (at-least-once processing with idempotent writes).
@Scheduled with fixedDelay (runs never overlap), every 3 hours, configurable and switchable via app.update.*;
an AtomicBoolean prevents a manual trigger from overlapping a scheduled run.
Limitation: deltas are kept about a day, so downtime longer than that needs a manual partition extraction;
the job logs a warning when it detects such a gap.

## 22. Stats cover a 14-day window anchored on the newest match
complete_matches keeps matches from the 14 days before the latest start_time (app.stats.window-days), so every
summary table is windowed through one shared base. Anchoring on the newest match (not "now") keeps the window full
even if updates pause. stats_refresh records window_start and window_end.

## 23. Hero trends from daily stats
hero_daily_stats (hero x tier x day) allows comparing the last 7 days with the 7 before for any rank filter.
RISING / FALLING when the win rate changes by more than 1.5 points, NOT_ENOUGH_DATA when either week has under
500 games. Rules live in MetaCalculator with unit tests (9 tests including the exact 1.5 boundary).

## Known limitations
- New heroes: Baba (released 2026-10-06) climbed from 36% to 58% daily win rate in a week while the 14-day
  average shows 51%. Tiers lag for heroes released inside the window.
- The fixed 1.5-point trend threshold ignores sample size; a confidence-based threshold would be more precise.
- The update job's lock does not cover a separately triggered refresh; one shared lock would prevent overlap.
- Patch-aware stats need a reliable patch-date source (separate issue).

## 24. Backend runs in Docker
Multi-stage Dockerfile: Maven + JDK 17 build stage, JRE 17 run stage with only the jar (420 MB, mostly the JRE
and DuckDB's native libraries). The official Maven image avoids mvnw line-ending problems on Windows. Not Alpine:
DuckDB's native code needs glibc. Runs as a non-root user. Dependencies are cached in their own layer
(71 s download vs 3.8 s compile). Compose passes configuration through environment variables (service names
postgres/redis instead of localhost), waits for PostgreSQL with a pg_isready healthcheck, and restarts the app
unless stopped. For coding sessions, start only postgres and redis and run the app locally.

## 25. Data retention and index cleanup
Measured first: item_purchases was 14 GB, of which 5.6 GB rows, an 8.1 GB primary key (about 2x bloated from
out-of-order loading) and a 603 MB item_id index with 0 uses since stats moved to summary tables.
- V5 drops the unused index.
- Retention deletes matches older than 21 days (relative to the newest match; above the 14-day stats window) in
  batches of 2,000 matches, one transaction each, children before parents. Runs in the update job after ingestion.
  First run: 140,304 matches, 1.68M players, 29.1M item purchases deleted in 51 s (~570k rows/s via primary keys).
- One-time VACUUM (FULL, ANALYZE): database 15 GB -> 8.2 GB, primary key 8.1 GB -> 3.25 GB.
Steady state: about 21 days of data, ~8 GB. If index bloat returns, REINDEX INDEX CONCURRENTLY rebuilds it
without locking the table.

## 26. Production hardening
- Dev endpoints (extract, ingest-all, refresh-stats, run-update, retention, assets-summary) only exist with the
  "dev" profile (@Profile). Production runs without it; the scheduler still runs. Local coding:
  `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`. Verified: 404 without the profile.
- PostgreSQL, Redis and the backend are published on 127.0.0.1 only. Docker's published ports bypass the host
  firewall (ufw), so binding to localhost is what actually keeps them off the internet.
- Redis requires a password (REDIS_PASSWORD in infra/.env). Verified: NOAUTH without it.
- Actuator exposes only health without details; error responses include no stack traces or exception messages.
- Unknown hero IDs return 404 before touching the cache or database. Validation fails open if assets could not
  be loaded at startup, so an outage of the assets API does not take the whole API down.

  ## 27. Integration tests and CI
Testcontainers starts a throwaway PostgreSQL 17 and Redis 7.4 (singleton containers shared by all test classes);
Flyway builds the schema from scratch on every run. AssetsService is mocked so tests never depend on the internet.
Test data is generated with DuckDB (TestData) with known outcomes, so expected stats can be computed by hand.
Covered: ingestion idempotency, refresh + meta/items/builds numbers and rank filtering, retention (deletes old,
keeps recent), security (dev endpoints 404 without profile, unknown hero 404, invalid minTier 400).
20 tests in ~24 s. GitHub Actions runs them plus a Docker build on every push to main and every PR;
Dependabot proposes weekly updates for Maven, Docker and Actions.

## 28. Caddy in front, memory limits
Caddy is the only service exposed to the internet (80/443); it obtains HTTPS certificates automatically,
returns 404 itself for /api/dev/* and /actuator/* (except health) as a second layer behind the dev profile,
adds security headers (HSTS, CSP default-src 'none', nosniff, frame denial, no-referrer) and removes Server/Via.
Memory limits sized for an 8 GB server: PostgreSQL 3 GB (shared_buffers 1 GB), backend 3 GB
(MaxRAMPercentage 75), Redis 256 MB (maxmemory 200 MB, allkeys-lru), Caddy 128 MB.
SPRING_PROFILES_ACTIVE is empty by default; dev is only set temporarily on the server for the initial
data load, with the calls made from the server itself.

## 29. Shared window tables in the stats refresh
Item stats and build stats each grouped raw purchases into "first buy per player and item";
item stats did it over all 21 days of retention before filtering to the 14-day window.
Now the refresh builds window_players and window_first_buys once (filtered to the window first)
and every step reads them. Results are identical (integration tests check exact values);
the laptop refresh went from 328 s to 164 s. The index on complete_matches was dropped:
the large hash joins don't use it.

## 30. Complete 12-item builds are not shown as a ranking
Measured on the server (21 days of data): Paige had 39,275 distinct 12-item builds among 42,568
players with a full inventory (92% unique); the most repeated build had 129 games and 7 builds
had 30+. Warden (rarely picked) had 11,181 distinct builds, a top build of 26 games and none
with 30+. The site shows phase-based item stats instead.