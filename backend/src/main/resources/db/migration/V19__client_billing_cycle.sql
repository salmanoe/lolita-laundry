-- Per-client billing cycle (monthly cut-off day).
--
-- A billing period was implicitly a calendar month: monthly_billings carried only
-- (period_year, period_month) and membership was the month of the order date. One client bills on
-- a contractual cut-off instead — the invoice covers the 26th of the previous month through the
-- 25th of this one.
--
-- A cut-off cycle still yields exactly one period per month, labelled by the month it ENDS in
-- (26 Jul - 25 Aug 2026 is the "2026-08" period). So (period_year, period_month) stays the
-- period's identity: the billing number BILL-{CODE}-{YYYYMM}, the UNIQUE constraint below and the
-- roll-forward-to-the-next-period logic are all unaffected. Only two things change:
--   1. clients.billing_cycle_day  — the mapping from an order date onto a period
--   2. monthly_billings.period_start / period_end — the period's actual calendar range
--
-- period_start/period_end follow the same freeze rule as the bank_*/company_* snapshot (V10): a
-- DRAFT re-resolves them from the client's current cycle on every sync, so switching a client's
-- cut-off realigns its open draft; ISSUE freezes them. They are stored rather than recomputed at
-- render time because "Perbarui Semua PDF" re-renders ISSUED/PAID billings too, and recomputing
-- would silently rewrite the period wording on an invoice already sent.

-- ── 1. The per-client setting ──
-- NULL = plain calendar month, which is every existing client, so there is nothing to backfill.
-- Capped at 28 so a period end never has to be clamped to a short February.
--
-- INTEGER, not SMALLINT, even though the value is 1-28: the entity field is `Integer`, which
-- Hibernate maps to int4, and `ddl-auto: validate` rejects an int2 column against it at startup.
-- This is the same trap that forced V6__monthly_billing_period_int.sql to widen
-- monthly_billings.period_year/period_month. Do not "optimise" this back to SMALLINT — the range
-- is enforced by the CHECK below, not by the storage type.
ALTER TABLE clients
    ADD COLUMN billing_cycle_day INTEGER
        CONSTRAINT clients_billing_cycle_day_check CHECK (billing_cycle_day BETWEEN 1 AND 28);

COMMENT ON COLUMN clients.billing_cycle_day IS
    'Monthly billing cut-off day; NULL = plain calendar month. Cut-off D means the period runs from D+1 of the previous month through D, labelled by its end month.';

-- ── 2. The period's calendar range, frozen on each billing ──
ALTER TABLE monthly_billings
    ADD COLUMN period_start DATE,
    ADD COLUMN period_end   DATE;

-- Every existing billing is a calendar month: 1st through the last day.
UPDATE monthly_billings
SET period_start = make_date(period_year, period_month, 1),
    period_end   = (make_date(period_year, period_month, 1) + INTERVAL '1 month' - INTERVAL '1 day')::date
WHERE period_start IS NULL;

ALTER TABLE monthly_billings
    ALTER COLUMN period_start SET NOT NULL,
    ALTER COLUMN period_end SET NOT NULL;

ALTER TABLE monthly_billings
    ADD CONSTRAINT monthly_billings_period_range_check CHECK (period_start <= period_end);

COMMENT ON COLUMN monthly_billings.period_start IS
    'First date the period covers, from the client billing cycle. Re-resolved while DRAFT, frozen at ISSUE.';
COMMENT ON COLUMN monthly_billings.period_end IS
    'Last date the period covers, from the client billing cycle. Re-resolved while DRAFT, frozen at ISSUE.';
