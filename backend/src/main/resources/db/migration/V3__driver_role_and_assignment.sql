-- ============================================================
-- Phase 2 extension — Driver delivery feature.
-- Adds the DRIVER role and an order → driver assignment.
-- Managed by Flyway. Never modify this file after deployment.
--
-- NOTE: this takes the V3 slot. The Phase 3 Spring Modulith
-- event_publication table moves to V4 (see CLAUDE.md).
-- ============================================================

-- ── Allow the DRIVER role (drivers log in, see only orders assigned to them) ──
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('OWNER', 'STAFF', 'DRIVER'));

-- ── Order → driver assignment (nullable: an order may be unassigned) ──
ALTER TABLE orders ADD COLUMN assigned_driver_id BIGINT REFERENCES users(id);

CREATE INDEX idx_orders_assigned_driver ON orders (assigned_driver_id);
