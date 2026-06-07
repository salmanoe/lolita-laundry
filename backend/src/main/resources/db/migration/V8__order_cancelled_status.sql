-- ============================================================
-- Auto-billing — add the CANCELLED (void) order status.
--
-- Orders now land on the monthly billing at RECEIVED, so a mistaken order
-- (the public form is unauthenticated) needs a way off the bill. CANCELLED is
-- a terminal off-ramp from RECEIVED/PROCESSING/DONE (a DELIVERED order cannot
-- be canceled). The order_status_history table has no status CHECK, so only
-- the orders constraint is widened.
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check
    CHECK (status IN ('RECEIVED', 'PROCESSING', 'DONE', 'DELIVERED', 'CANCELLED'));