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