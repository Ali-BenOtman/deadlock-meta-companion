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

