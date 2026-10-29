-- Per-client Treatment (×2) eligibility.
--
-- Treatment — a whole-order ×2 price multiplier (orders.pricing_multiplier) — used to be gated to
-- PER_DEPARTMENT clients, because PBS happened to be the only client that buys it. That coupled two
-- unrelated things: how a client is billed (one invoice per department) and whether it buys the
-- Treatment service. This flag decouples them, so a COMBINED client can be offered Treatment too.
--
-- Everything downstream is already client-neutral: the multiplier is snapshotted on the order and
-- baked into each line subtotal, so billing, the order invoice and reports need no change. Turning
-- the flag off later never touches existing orders.

ALTER TABLE clients
    ADD COLUMN treatment_enabled BOOLEAN NOT NULL DEFAULT false;

-- Preserve today's behaviour: every PER_DEPARTMENT client (PBS) keeps Treatment.
UPDATE clients
SET treatment_enabled = true
WHERE billing_mode = 'PER_DEPARTMENT';

COMMENT ON COLUMN clients.treatment_enabled IS
    'Whether orders for this client may use Treatment pricing (whole-order x2). Independent of billing_mode.';
