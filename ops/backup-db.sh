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
# Requires: postgresql-client (pg_dump), gzip, the AWS CLI (`sudo apt install -y awscli`)
# for the R2 upload, and `age` (`sudo apt install -y age`) for encrypting the off-box copy.
# R2 credentials + endpoint are read from .env.prod (never hard-coded here).
#
# ── Encryption (the off-box copy) ─────────────────────────────────────────
# The dump contains customer identities, commercially sensitive per-client prices, and full
# billing history. The local copy stays on the VPS (already the trust boundary), but the copy
# pushed to R2 is client-side encrypted with `age` so a leaked/overscoped R2 key alone cannot
# read it. Set AGE_RECIPIENT in .env.prod to an age PUBLIC key (age1...); keep the matching
# PRIVATE key OFF the VPS (e.g. in a password manager). Generate a keypair once with:
#   age-keygen -o key.txt      # prints "Public key: age1..." — put that in AGE_RECIPIENT,
#                              # store key.txt somewhere safe and OFF this server.
# Restore an R2 backup with the private key present:
#   aws s3 cp s3://.../lolita_YYYYMMDD.dump.gz.age . --endpoint-url ...   # download
#   age -d -i key.txt lolita_YYYYMMDD.dump.gz.age | gunzip | pg_restore -d lolita_laundry
# If AGE_RECIPIENT is unset (or `age` is missing) the R2 upload is SKIPPED — the script never
# pushes an unencrypted dump off-box. The local plaintext copy is still written for fast restore.
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
ENC_FILE="${FILE}.age"                 # the encrypted copy pushed to R2
ENC_PATH="${BACKUP_DIR}/${ENC_FILE}"
# Run pg_dump as the postgres OS user (peer auth) if not already; keeps creds out of this script.
RUNAS=(); [ "$(id -un)" = "postgres" ] || RUNAS=(sudo -u postgres)

log() { echo "[$(date '+%F %T')] $*"; }

mkdir -p "$BACKUP_DIR"

# ── 1. Dump (custom format) + gzip ────────────────────────────────────────
log "dumping ${DB_NAME} -> ${LOCAL_PATH}"
"${RUNAS[@]}" pg_dump -Fc "$DB_NAME" | gzip > "$LOCAL_PATH"
SIZE="$(du -h "$LOCAL_PATH" | cut -f1)"
log "wrote ${FILE} (${SIZE})"

# ── 2. Encrypt for off-box storage, then upload to R2 ─────────────────────
# Creds + the age recipient are sourced literally from .env.prod and never printed.
R2_ACCESS_KEY="$(sudo grep '^R2_ACCESS_KEY=' "$ENV_FILE" | cut -d= -f2-)"
R2_SECRET_KEY="$(sudo grep '^R2_SECRET_KEY=' "$ENV_FILE" | cut -d= -f2-)"
AGE_RECIPIENT="$(sudo grep '^AGE_RECIPIENT=' "$ENV_FILE" 2>/dev/null | cut -d= -f2- || true)"

if [ -z "$AGE_RECIPIENT" ] || ! command -v age >/dev/null 2>&1; then
  # Fail closed on the off-box copy: never push an unencrypted dump to R2. The local
  # plaintext backup (step 1) is unaffected — the box itself is the trust boundary.
  log "WARNING: AGE_RECIPIENT unset or 'age' not installed — skipped R2 upload (encryption required for off-box copy)"
elif [ -n "$R2_ACCESS_KEY" ] && command -v aws >/dev/null 2>&1; then
  log "encrypting -> ${ENC_FILE}"
  age -r "$AGE_RECIPIENT" -o "$ENC_PATH" "$LOCAL_PATH"
  AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY" AWS_SECRET_ACCESS_KEY="$R2_SECRET_KEY" AWS_DEFAULT_REGION=auto \
    aws s3 cp "$ENC_PATH" "s3://${R2_BUCKET}/${R2_PREFIX}/${ENC_FILE}" --endpoint-url "$R2_ENDPOINT" --only-show-errors
  log "uploaded encrypted backup to r2://${R2_BUCKET}/${R2_PREFIX}/${ENC_FILE}"
  rm -f "$ENC_PATH"   # the encrypted artifact only needed to exist for the upload
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
    | awk '{print $NF}' | grep -E '^lolita_[0-9]{8}\.dump\.gz\.age$' | while read -r key; do
      d="${key#lolita_}"; d="${d%.dump.gz.age}"                   # YYYYMMDD
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
