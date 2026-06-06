-- ============================================================
-- Phase 2 extension reversal — Open delivery pool.
-- Drops the order → driver assignment introduced in V3.
-- Drivers now share one pool: every driver sees every order
-- not yet delivered and picks what to deliver. The DRIVER
-- role (V3 CHECK constraint) stays.
-- Managed by Flyway. Never modify this file after deployment.
--
-- NOTE: this takes the V4 slot. The Phase 3 Spring Modulith
-- event_publication table moves to V5 (see CLAUDE.md).
-- ============================================================

DROP INDEX IF EXISTS idx_orders_assigned_driver;
ALTER TABLE orders DROP COLUMN IF EXISTS assigned_driver_id;