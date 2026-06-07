-- ============================================================
-- Item categories removed; item→department mapping introduced.
--
-- Design pivot (Option B): an order no longer belongs to a single department. Each item is
-- mapped to a department per client (managed in "Atur Harga"), order line items carry a
-- department snapshot, and a single order is split across its departments' monthly billings.
-- The global item-category lookup is dropped — for PER_DEPARTMENT clients the department is
-- the grouping dimension; COMBINED clients use a flat item list.
-- ============================================================

-- ── Drop the item-category lookup (and its FK on item_master) ──
ALTER TABLE item_master DROP CONSTRAINT IF EXISTS fk_item_master_category;
ALTER TABLE item_master DROP COLUMN category_id;
DROP TABLE item_categories;

-- ── Per-client item→department mapping (the "Atur Harga" assignment) ──
--    Only meaningful for PER_DEPARTMENT clients (e.g. PBS). One department per item per client.
CREATE TABLE client_item_departments (
    id            BIGSERIAL PRIMARY KEY,
    client_id     BIGINT NOT NULL REFERENCES clients(id),
    item_id       BIGINT NOT NULL REFERENCES item_master(id),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    UNIQUE (client_id, item_id)
);

CREATE INDEX idx_client_item_dept_client ON client_item_departments (client_id);

-- ── Line-level department snapshot (set at order creation from the mapping; null for COMBINED) ──
ALTER TABLE order_line_items ADD COLUMN department_id BIGINT REFERENCES departments(id);

-- Backfill existing line items from the order's (soon-to-be-dropped) department, for continuity.
UPDATE order_line_items li
   SET department_id = o.department_id
  FROM orders o
 WHERE li.order_id = o.id
   AND o.department_id IS NOT NULL;

-- ── Retire the order-level department — department now lives on each line item ──
ALTER TABLE orders DROP COLUMN department_id;
