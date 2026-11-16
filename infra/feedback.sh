#!/usr/bin/env bash
# Prints the newest feedback messages and the average rating.
# Usage: bash infra/feedback.sh [number of messages, default 30]
# Messages come from strangers, so control characters are replaced with spaces before they reach the terminal.
set -euo pipefail

count="${1:-30}"
case "$count" in
    ''|*[!0-9]*) echo "Usage: bash infra/feedback.sh [number of messages]" >&2; exit 1 ;;
esac

docker exec deadlock-postgres psql -U deadlock -d deadlock \
    -c "SELECT count(*) AS messages, count(rating) AS rated, round(avg(rating), 2) AS average_rating FROM feedback;"
docker exec deadlock-postgres psql -U deadlock -d deadlock -x \
    -c "SELECT created_at, rating, regexp_replace(message, '[[:cntrl:]]', ' ', 'g') AS message FROM feedback ORDER BY created_at DESC LIMIT $count;"