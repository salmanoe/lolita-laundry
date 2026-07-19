#!/usr/bin/env bash
#
# Nightly logical backup of the self-hosted Lolita Laundry Postgres → local disk + Cloudflare R2.
#
# Chosen backup tier: "nightly now, PITR later" (see the migration plan / Deployment wiki).
# This is a simple, near-zero-maintenance logical dump. It does NOT provide point-in-time
# recovery — worst-case data loss is up to ~24h (since the last nightly run). A pgBackRest +
# WAL-archiving follow-up can add PITR later.
#
# Deploy: copy to /home/ubuntu/lolita-laundry/backup-db.sh, `chmod +x`, and cron it nightly:
#   30 2 * * *  /home/ubuntu/lolita-laundry/backup-db.sh >> /home/ubuntu/backups/backup.log 2>&1
#
# Requires: postgresql-client (pg_dump), gzip, and the AWS CLI (`sudo apt install -y awscli`)
# for the R2 upload. R2 credentials + endpoint are read from .env.prod (never hard-coded here).
#
set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────
DB_NAME="lolita_laundry"
DB_USER="postgres"                 # local peer auth via `sudo -u postgres`; see RUNAS below
BACKUP_DIR="/home/ubuntu/backups"
ENV_FILE="/home/ubuntu/lolita-laundry/.env.prod"
R2_ENDPOINT="https://2a48f3351e414c179868bf5ef6ca0a50.r2.cloudflarestorage.com"
R2_BUCKET="lolita-laundry"
R2_PREFIX="db-backups"
LOCAL_KEEP=7                       # keep this many most-recent dumps on local disk
R2_KEEP_DAILY_DAYS=14              # on R2: keep every dump from the last N days ...
R2_KEEP_MONTHLY_DAYS=365           # ... plus first-of-month dumps for the last N days
DISK_WARN_PCT=80                   # warn if the root filesystem exceeds this

TS="$(date +%Y%m%d)"
FILE="lolita_${TS}.dump.gz"
LOCAL_PATH="${BACKUP_DIR}/${FILE}"
# Run pg_dump as the postgres OS user (peer auth) if not already; keeps creds out of this script.
RUNAS=(); [ "$(id -un)" = "postgres" ] || RUNAS=(sudo -u postgres)

log() { echo "[$(date '+%F %T')] $*"; }

mkdir -p "$BACKUP_DIR"

# ── 1. Dump (custom format) + gzip ────────────────────────────────────────
log "dumping ${DB_NAME} -> ${LOCAL_PATH}"
"${RUNAS[@]}" pg_dump -Fc "$DB_NAME" | gzip > "$LOCAL_PATH"
SIZE="$(du -h "$LOCAL_PATH" | cut -f1)"
log "wrote ${FILE} (${SIZE})"

# ── 2. Upload to R2 (creds sourced literally from .env.prod, never printed) ─
R2_ACCESS_KEY="$(sudo grep '^R2_ACCESS_KEY=' "$ENV_FILE" | cut -d= -f2-)"
R2_SECRET_KEY="$(sudo grep '^R2_SECRET_KEY=' "$ENV_FILE" | cut -d= -f2-)"
if [ -n "$R2_ACCESS_KEY" ] && command -v aws >/dev/null 2>&1; then
  AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY" AWS_SECRET_ACCESS_KEY="$R2_SECRET_KEY" AWS_DEFAULT_REGION=auto \
    aws s3 cp "$LOCAL_PATH" "s3://${R2_BUCKET}/${R2_PREFIX}/${FILE}" --endpoint-url "$R2_ENDPOINT" --only-show-errors
  log "uploaded to r2://${R2_BUCKET}/${R2_PREFIX}/${FILE}"
else
  log "WARNING: aws CLI missing or R2 creds empty — skipped R2 upload (local backup still made)"
fi

# ── 3. Prune local (keep most-recent LOCAL_KEEP) ──────────────────────────
ls -1t "${BACKUP_DIR}"/lolita_*.dump.gz 2>/dev/null | tail -n +$((LOCAL_KEEP + 1)) | while read -r old; do
  log "pruning local $(basename "$old")"; rm -f "$old"
done

# ── 4. Prune R2 (keep last 14 days daily + first-of-month for a year) ──────
if [ -n "${R2_ACCESS_KEY:-}" ] && command -v aws >/dev/null 2>&1; then
  now=$(date +%s)
  AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY" AWS_SECRET_ACCESS_KEY="$R2_SECRET_KEY" AWS_DEFAULT_REGION=auto \
    aws s3 ls "s3://${R2_BUCKET}/${R2_PREFIX}/" --endpoint-url "$R2_ENDPOINT" \
    | awk '{print $NF}' | grep -E '^lolita_[0-9]{8}\.dump\.gz$' | while read -r key; do
      d="${key#lolita_}"; d="${d%.dump.gz}"                       # YYYYMMDD
      age_days=$(( (now - $(date -d "$d" +%s)) / 86400 ))
      keep=false
      [ "$age_days" -le "$R2_KEEP_DAILY_DAYS" ] && keep=true
      [ "${d:6:2}" = "01" ] && [ "$age_days" -le "$R2_KEEP_MONTHLY_DAYS" ] && keep=true
      if [ "$keep" = false ]; then
        log "pruning r2 ${key} (age ${age_days}d)"
        AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY" AWS_SECRET_ACCESS_KEY="$R2_SECRET_KEY" AWS_DEFAULT_REGION=auto \
          aws s3 rm "s3://${R2_BUCKET}/${R2_PREFIX}/${key}" --endpoint-url "$R2_ENDPOINT" --only-show-errors
      fi
  done
fi

# ── 5. Disk-usage guard ───────────────────────────────────────────────────
USE_PCT="$(df --output=pcent / | tail -1 | tr -dc '0-9')"
if [ "${USE_PCT:-0}" -ge "$DISK_WARN_PCT" ]; then
  log "WARNING: root filesystem at ${USE_PCT}% (>= ${DISK_WARN_PCT}%) — investigate disk usage"
fi

log "backup complete"
