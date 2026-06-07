-- ============================================================
-- Phase 3 — denormalize the department name onto monthly_billings.
--
-- The monthly invoice PDF prints the department (for PER_DEPARTMENT clients
-- like PBS), but the row only stored department_id — the name was pulled from
-- the orders at generation time. To re-render a billing's PDF on demand (the
-- "Perbarui PDF" feature) without re-reading orders — and for ISSUED/PAID
-- billings too — the name must live on the billing row itself.
--
-- Nullable: COMBINED clients have no department. Backfilled from departments.
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

ALTER TABLE monthly_billings ADD COLUMN department_name VARCHAR(100);

UPDATE monthly_billings mb
SET department_name = d.name
FROM departments d
WHERE mb.department_id = d.id;