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