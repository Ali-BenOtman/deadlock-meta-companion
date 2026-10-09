# Decisions



## 1. Use the data lake, not the REST API, for bulk stats

The REST API is rate-limited; the data lake gives bulk Parquet files with no credentials.

We read only needed files and columns, so the 486 GB table is never downloaded in full.

