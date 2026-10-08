\# Data notes



\## Source

Community Deadlock API data lake (not affiliated with Valve).

Hourly Parquet exports: https://deadlock-api.com/data-dumps



\## Main table: match\_player

\- One row per player per match. \~361M rows, \~486 GB, split into many files.

\- Never query the whole table; read single files with read\_parquet().

\- File names show build dates, not match dates. Low part numbers hold the oldest matches.

\- "residual" files mix late additions from many dates.

\- A row can appear twice (base + newer file): keep the newest per (match\_id, account\_id).



\## Columns we use

\- match\_id, start\_time, duration\_s

\- match\_mode: use 'Ranked' only for the MVP

\- average\_badge: match's average rank (filled for recent ranked matches; empty in 2024 data)

\- hero\_id, team, won

\- items.item\_id, items.game\_time\_s, items.sold\_time\_s: parallel lists, same position = same item.

&#x20; sold\_time\_s = 0 means never sold. List may include ability upgrades, not just shop items.



\## Open questions

\- Lookup lists for hero names, item names and badge values

\- How to tell shop items from abilities in the items list

\- Which files hold the bulk of recent ranked matches

