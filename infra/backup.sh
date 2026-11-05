#!/usr/bin/env bash
# Nightly PostgreSQL backup: compressed dump, validated, newest KEEP copies kept.
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-$HOME/backups}"
KEEP="${KEEP:-3}"
CONTAINER="deadlock-postgres"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
PARTIAL="$BACKUP_DIR/.deadlock-$STAMP.partial"
FINAL="$BACKUP_DIR/deadlock-$STAMP.dump"

mkdir -p "$BACKUP_DIR"

# Never run two backups at once.
exec 9>"$BACKUP_DIR/.backup.lock"
flock -n 9 || { echo "$(date -u +%FT%TZ) another backup is running, skipping"; exit 0; }

# A half-written file is never kept as a backup.
trap 'rm -f "$PARTIAL"' EXIT

echo "$(date -u +%FT%TZ) backup started"
/usr/bin/docker exec "$CONTAINER" pg_dump -U deadlock -d deadlock -Fc -Z 3 > "$PARTIAL"

# The file must not be empty, and the archive must be readable.
[ -s "$PARTIAL" ]
/usr/bin/docker exec -i "$CONTAINER" pg_restore --list < "$PARTIAL" > /dev/null

mv "$PARTIAL" "$FINAL"
echo "$(date -u +%FT%TZ) backup finished: $FINAL ($(du -h "$FINAL" | cut -f1))"

# Keep only the newest KEEP backups.
ls -1t "$BACKUP_DIR"/deadlock-*.dump | tail -n +"$((KEEP + 1))" | xargs -r rm -f --