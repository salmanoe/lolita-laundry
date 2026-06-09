-- ============================================================
-- KI-6 — optimistic locking on the mutable aggregates.
--
-- orders and monthly_billings are read-modify-write aggregates. UNIQUE
-- constraints were the only concurrency backstop, so two transactions editing
-- the same row could lose an update silently. A JPA @Version column makes a
-- concurrent stale save throw OptimisticLockingFailureException, which the web
-- layer translates to 409 and the async billing listener gets retried for. The
-- billing event thread is already single-threaded (KI-0/KI-4), so this is mainly
-- the durable, multi-instance-safe backstop for the whole lost-update class.
--
-- Existing rows start at version 0 (DEFAULT). New inserts: Hibernate seeds 0.
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

ALTER TABLE orders           ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE monthly_billings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
