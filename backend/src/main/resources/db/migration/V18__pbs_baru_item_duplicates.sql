-- ============================================================
-- One-off data migration: duplicate PBS's item catalogue with a " (BARU)" suffix.
--
-- Pasar Baru Square Hotel wants a parallel set of every item it orders, named identically plus
-- " (BARU)" (e.g. 'Sheet King' → 'Sheet King (BARU)'), at the same price — to distinguish new
-- linen from existing stock on the order form. Data only: no schema change.
--
-- Scope is PBS alone. item_master is a global catalogue, but the order form lists only a client's
-- *priced* items, so the duplicates never surface for the other clients.
-- ============================================================

-- ── 1. The duplicated items ──
--    Source set = active items PBS has at least one price row for. Already-suffixed names are
--    excluded so this can never produce "X (BARU) (BARU)". unit_id is carried over; `active`
--    defaults TRUE.
INSERT INTO item_master (name, unit_id)
SELECT i.name || ' (BARU)', i.unit_id
FROM item_master i
WHERE i.active
  AND i.name NOT LIKE '% (BARU)'
  AND EXISTS (
      SELECT 1
      FROM client_price_lists p
      JOIN clients c ON c.id = p.client_id
      WHERE p.item_id = i.id
        AND c.client_code = 'PBS'
  )
ON CONFLICT (name) DO NOTHING;

-- ── 2. PBS price for each duplicate: the twin's CURRENT price ──
--    Price lookup is "latest row with effective_date <= order_date", so the new row is back-dated
--    to the earliest date PBS priced the original — otherwise a back-dated order would fall into
--    an unpriced gap. No history is falsified: a (BARU) item has no past orders.
INSERT INTO client_price_lists (client_id, item_id, price_per_unit, effective_date)
SELECT src.client_id, n.id, src.price_per_unit, src.first_date
FROM (
    SELECT DISTINCT ON (p.item_id)
           p.client_id,
           p.item_id,
           p.price_per_unit,
           MIN(p.effective_date) OVER (PARTITION BY p.item_id) AS first_date
    FROM client_price_lists p
    JOIN clients c ON c.id = p.client_id
    WHERE c.client_code = 'PBS'
    ORDER BY p.item_id, p.effective_date DESC
) src
JOIN item_master o ON o.id = src.item_id
JOIN item_master n ON n.name = o.name || ' (BARU)'
WHERE o.name NOT LIKE '% (BARU)'
ON CONFLICT (client_id, item_id, effective_date) DO NOTHING;

-- ── 3. Item→department mapping ──
--    Mandatory, not cosmetic: PBS is PER_DEPARTMENT and order creation rejects an item with no
--    department mapping. Each duplicate inherits its twin's department.
INSERT INTO client_item_departments (client_id, item_id, department_id)
SELECT d.client_id, n.id, d.department_id
FROM client_item_departments d
JOIN clients     c ON c.id = d.client_id AND c.client_code = 'PBS'
JOIN item_master o ON o.id = d.item_id
JOIN item_master n ON n.name = o.name || ' (BARU)'
WHERE o.name NOT LIKE '% (BARU)'
ON CONFLICT (client_id, item_id) DO NOTHING;
