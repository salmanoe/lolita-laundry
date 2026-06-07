-- ============================================================
-- Phase 3 fix — widen monthly_billings period columns to INTEGER.
--
-- V1 declared period_year / period_month as SMALLINT, but the JPA
-- entity (MonthlyBillingJpaEntity) and domain map them as `int`
-- (Types#INTEGER). Hibernate ddl-auto: validate (dev/prod) rejects the
-- SMALLINT/INTEGER mismatch on startup. Tests never caught it because
-- they use create-drop (Hibernate generates the columns as INTEGER).
--
-- Forward ALTER instead of editing V1: V1 is the deployed foundation.
-- The CHECK on period_month (1..12) is preserved by the type change.
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

ALTER TABLE monthly_billings
    ALTER COLUMN period_year  TYPE INTEGER,
    ALTER COLUMN period_month TYPE INTEGER;