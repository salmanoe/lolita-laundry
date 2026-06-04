-- ============================================================
-- Reference data → lookup tables (dynamic CRUD)
-- Converts the ItemUnit, ItemCategory and ClientType enums (string columns + CHECK
-- constraints) into managed lookup tables referenced by FK id.
-- BillingMode stays a fixed enum (it drives billing logic) — untouched here.
-- ============================================================

-- ── Lookup tables ──
CREATE TABLE item_units (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(10)  NOT NULL UNIQUE,
    display_name VARCHAR(50)  NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE item_categories (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(20)  NOT NULL UNIQUE,
    display_name VARCHAR(50)  NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE client_types (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(20)  NOT NULL UNIQUE,
    display_name VARCHAR(50)  NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ── Seed the canonical values (Indonesian display names) — runs in all environments ──
INSERT INTO item_units (code, display_name, sort_order) VALUES
    ('PCS', 'Pcs', 1), ('KG', 'Kg', 2), ('M2', 'm²', 3), ('SET', 'Set', 4);

INSERT INTO item_categories (code, display_name, sort_order) VALUES
    ('BED_LINEN', 'Linen Tempat Tidur', 1),
    ('BATH',      'Kamar Mandi',        2),
    ('FB',        'F&B',                3),
    ('UNIFORM',   'Seragam/Tamu',       4),
    ('OTHER',     'Lainnya',            5);

INSERT INTO client_types (code, display_name, sort_order) VALUES
    ('HOTEL', 'Hotel', 1), ('RESTAURANT', 'Restoran', 2), ('SPA', 'Spa', 3),
    ('CLINIC', 'Klinik', 4), ('OTHER', 'Lainnya', 5);

-- ── item_master: replace unit/category strings with FK ids ──
ALTER TABLE item_master ADD COLUMN unit_id     BIGINT;
ALTER TABLE item_master ADD COLUMN category_id BIGINT;

UPDATE item_master SET unit_id     = (SELECT id FROM item_units      WHERE code = item_master.unit);
UPDATE item_master SET category_id = (SELECT id FROM item_categories WHERE code = item_master.category);

ALTER TABLE item_master DROP COLUMN unit;       -- drops the old CHECK constraint with it
ALTER TABLE item_master DROP COLUMN category;

ALTER TABLE item_master ALTER COLUMN unit_id     SET NOT NULL;
ALTER TABLE item_master ALTER COLUMN category_id SET NOT NULL;
ALTER TABLE item_master ADD CONSTRAINT fk_item_master_unit     FOREIGN KEY (unit_id)     REFERENCES item_units(id);
ALTER TABLE item_master ADD CONSTRAINT fk_item_master_category FOREIGN KEY (category_id) REFERENCES item_categories(id);

-- ── clients: replace client_type string with FK id (billing_mode stays a string enum) ──
ALTER TABLE clients ADD COLUMN client_type_id BIGINT;

UPDATE clients SET client_type_id = (SELECT id FROM client_types WHERE code = clients.client_type);

ALTER TABLE clients DROP COLUMN client_type;    -- drops the old CHECK constraint with it

ALTER TABLE clients ALTER COLUMN client_type_id SET NOT NULL;
ALTER TABLE clients ADD CONSTRAINT fk_clients_client_type FOREIGN KEY (client_type_id) REFERENCES client_types(id);
