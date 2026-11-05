# Backups

The PostgreSQL database is dumped every night by `infra/backup.sh`. The newest three dumps are kept in
`/home/deploy/backups` on the server (about 1 GB each); older ones are deleted. The Redis cache is not
backed up: it only holds cached answers and is rebuilt from the database.

## What was measured (2026-11-05)

- An 18 GB database dumps to about 980 MB in about 4 minutes (3:46 to 4:42 over three runs).
- A test restore into a separate container (1.5 GB memory, 2 CPU cores) took under 16 minutes.
- The restored copy had the same row counts in the four main tables (529,726 matches, 6,354,336 players,
  107,668,421 item purchases, 1,261,571 build stats) and the same 14 indexes as the live database.

## How the script works

- It writes to a hidden `.partial` file and renames it only after the dump finished and the archive could
  be listed with `pg_restore --list`, so a failed dump never replaces a good backup.
- A lock file stops two backups from running at once.
- Not yet tested: the lock (two simultaneous runs) and the failure path (a dump that fails halfway).

## Schedule

The schedule lives in the `deploy` user's crontab on the server, not in this repository:

```
30 3 * * * bash /home/deploy/deadlock-meta-companion/infra/backup.sh >> /home/deploy/backups/backup.log 2>&1
```

That is 03:30 server time (Europe/Berlin). Data updates start three hours after the previous one ends, so
their start time drifts and a backup sometimes overlaps one. That is harmless (a dump reads a consistent
snapshot) but both run slower on the shared CPU.

## Restore into a separate container (tested)

Use this to check that a backup works. It never touches the live database. Do not run it while a data
update is running: the server has no swap, so keep the memory limit.

```bash
docker run -d --name deadlock-restore-test --memory 1500m --cpus 2 -e POSTGRES_PASSWORD=testonly \
  -v restore-test-data:/var/lib/postgresql/data postgres:17 \
  -c shared_buffers=256MB -c maintenance_work_mem=256MB -c max_wal_size=4GB -c fsync=off
docker exec deadlock-restore-test createdb -U postgres deadlock_restore
docker cp ~/backups/<file>.dump deadlock-restore-test:/tmp/backup.dump
docker exec deadlock-restore-test pg_restore -U postgres -d deadlock_restore --no-owner --no-privileges -j 2 /tmp/backup.dump
```

Compare row counts and `SELECT count(*) FROM pg_indexes WHERE schemaname='public'` with the live database,
then remove the test container and volume:

```bash
docker rm -f deadlock-restore-test
docker volume rm restore-test-data
```

## Restore over the live database (not tested)

The steps below use the same dump format and the same `pg_restore` options as the tested restore, but they
have not been run against the live database. Read them before an emergency, not during one.

1. Stop the backend so nothing is writing: `docker compose -f infra/docker-compose.yml stop backend`
2. Recreate the database:
   `docker exec deadlock-postgres dropdb -U deadlock deadlock` then
   `docker exec deadlock-postgres createdb -U deadlock deadlock`
3. Copy the dump in and restore it:
   `docker cp ~/backups/<file>.dump deadlock-postgres:/tmp/backup.dump`, then
   `docker exec deadlock-postgres pg_restore -U deadlock -d deadlock --no-owner --no-privileges -j 2 /tmp/backup.dump`
4. Start the backend and recreate Redis so no old cached answers remain:
   `docker compose -f infra/docker-compose.yml up -d backend` and
   `docker compose -f infra/docker-compose.yml up -d --force-recreate redis`

## Limits

- The backups are on the same server as the database, so they protect against mistakes (a bad migration,
  an accidental delete) but not against losing the server. There is no off-server copy yet.
- The script checks that an archive is readable, not that it restores. Repeat the restore test now and
  then.
- Rebuilding from the data lake instead would need the public partitions to still exist and would take
  several hours of loading.