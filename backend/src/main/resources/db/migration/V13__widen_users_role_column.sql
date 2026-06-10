-- ============================================================
-- Widen users.role from VARCHAR(10) to VARCHAR(20).
--
-- The original column (V1) was VARCHAR(10), which cannot hold
-- 'SUPER_ADMIN' (11 chars) added in V12 — inserts/updates with
-- that role fail with "value too long for type character varying(10)".
-- Split from V12 because V12 was already applied; editing an applied
-- migration would break Flyway's checksum validation.
--
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20);
