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
