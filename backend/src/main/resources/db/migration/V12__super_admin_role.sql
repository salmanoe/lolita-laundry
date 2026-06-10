-- ============================================================
-- New SUPER_ADMIN role — top-level administrator.
--
-- SUPER_ADMIN can view all dashboards, manage system config
-- (users, master data, items) and perform billing adjustments.
-- OWNER becomes a business/analytics viewer (no admin screens,
-- no daily/operational dashboard). STAFF is unchanged.
--
-- Only widens the CHECK constraint — no data change.
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('SUPER_ADMIN', 'OWNER', 'STAFF', 'DRIVER'));
