-- ============================================================
-- Role model rework: 4 roles → 3 roles.
--
--   OWNER  → SUPER_ADMIN    (owners were full business admins)
--   STAFF  → FINANCE_STAFF  (unchanged permission set, renamed)
--   DRIVER → DAILY_STAFF    (absorbs the driver job + order entry)
--   SUPER_ADMIN             (unchanged)
--
-- Drop the CHECK first so the UPDATEs can move rows through the
-- old values, remap the data, then re-add the CHECK with the new
-- value set. role is already VARCHAR(20) (V13) — FINANCE_STAFF fits.
--
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

UPDATE users SET role = 'SUPER_ADMIN'   WHERE role = 'OWNER';
UPDATE users SET role = 'FINANCE_STAFF' WHERE role = 'STAFF';
UPDATE users SET role = 'DAILY_STAFF'   WHERE role = 'DRIVER';

ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('SUPER_ADMIN', 'FINANCE_STAFF', 'DAILY_STAFF'));
