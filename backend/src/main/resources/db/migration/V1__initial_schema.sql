-- ============================================================
-- Lolita Laundry — Initial Schema
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

-- ── Users (Lolita staff: OWNER and STAFF roles) ──
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    auth0_sub   VARCHAR(128) NOT NULL UNIQUE,  -- Auth0 'sub' claim, e.g. "auth0|abc123"
    full_name   VARCHAR(100) NOT NULL,
    role        VARCHAR(10)  NOT NULL CHECK (role IN ('OWNER', 'STAFF')),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Clients (generalized: hotels, restaurants, etc.) ──
CREATE TABLE clients (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL,
    client_code     VARCHAR(10)   NOT NULL UNIQUE,  -- e.g. PBS, AYI — used in order_number prefix
    client_type     VARCHAR(10)   NOT NULL DEFAULT 'HOTEL'
                        CHECK (client_type IN ('HOTEL','RESTAURANT','SPA','CLINIC','OTHER')),
    billing_mode    VARCHAR(20)   NOT NULL DEFAULT 'COMBINED'
                        CHECK (billing_mode IN ('COMBINED','PER_DEPARTMENT')),
    contact_person  VARCHAR(100),
    phone           VARCHAR(20),
    address         TEXT,
    order_token     UUID          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ── Departments (only relevant when billing_mode = PER_DEPARTMENT, e.g. PBS) ──
CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    client_id   BIGINT      NOT NULL REFERENCES clients(id),
    name        VARCHAR(100) NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (client_id, name)
);

-- ── Item master catalogue ──
CREATE TABLE item_master (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL UNIQUE,
    unit        VARCHAR(5)    NOT NULL CHECK (unit IN ('PCS','KG','M2','SET')),
    category    VARCHAR(10)   NOT NULL
                    CHECK (category IN ('BED_LINEN','BATH','FB','UNIFORM','OTHER')),
    active      BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ── Client price lists (append-only; never UPDATE or DELETE) ──
--    Query: WHERE client_id = ? AND item_id = ? AND effective_date <= :order_date
--           ORDER BY effective_date DESC LIMIT 1
CREATE TABLE client_price_lists (
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT          NOT NULL REFERENCES clients(id),
    item_id         BIGINT          NOT NULL REFERENCES item_master(id),
    price_per_unit  NUMERIC(12, 2)  NOT NULL CHECK (price_per_unit >= 0),
    effective_date  DATE            NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- One price per item per client per effective date (append-only: change price by
    -- inserting a row with a newer effective_date, never by editing an existing row).
    UNIQUE (client_id, item_id, effective_date)
);

CREATE INDEX idx_price_list_lookup
    ON client_price_lists (client_id, item_id, effective_date DESC);

-- ── Orders ──
CREATE TABLE orders (
    id                   BIGSERIAL PRIMARY KEY,
    order_number         VARCHAR(30)    NOT NULL UNIQUE, -- e.g. PBS-20260601-001
    client_id            BIGINT         NOT NULL REFERENCES clients(id),
    department_id        BIGINT         REFERENCES departments(id),
    order_date           DATE           NOT NULL DEFAULT CURRENT_DATE,
    due_date             DATE,
    status               VARCHAR(12)    NOT NULL DEFAULT 'RECEIVED'
                             CHECK (status IN ('RECEIVED','PROCESSING','DONE','DELIVERED')),
    pricing_multiplier   NUMERIC(4, 2)  NOT NULL DEFAULT 1.0,  -- 2.0 for PBS Treatment
    submitted_by_name    VARCHAR(100)   NOT NULL,               -- hotel staff name from public form
    notes                TEXT,
    created_by_user_id   BIGINT         REFERENCES users(id),   -- NULL when submitted via public token
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_client_status ON orders (client_id, status);
CREATE INDEX idx_orders_order_date    ON orders (order_date);

-- ── Order line items (immutable after creation) ──
CREATE TABLE order_line_items (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES orders(id),
    item_id         BIGINT          NOT NULL REFERENCES item_master(id),
    quantity        NUMERIC(10, 3)  NOT NULL CHECK (quantity > 0),  -- m² for Curtain/Vitrage
    price_at_order  NUMERIC(12, 2)  NOT NULL CHECK (price_at_order >= 0),  -- snapshot, never update
    subtotal        NUMERIC(14, 2)  NOT NULL  -- quantity * price_at_order * pricing_multiplier
);

-- ── Order status history ──
CREATE TABLE order_status_history (
    id              BIGSERIAL   PRIMARY KEY,
    order_id        BIGINT      NOT NULL REFERENCES orders(id),
    from_status     VARCHAR(12),              -- NULL for the initial RECEIVED entry
    to_status       VARCHAR(12) NOT NULL,
    changed_by_id   BIGINT      REFERENCES users(id),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes           TEXT
);

-- ── Delivery confirmation (filled by Lolita staff at delivery) ──
CREATE TABLE delivery_confirmations (
    id              BIGSERIAL    PRIMARY KEY,
    order_id        BIGINT       NOT NULL UNIQUE REFERENCES orders(id),
    delivered_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    recipient_name  VARCHAR(100) NOT NULL,   -- Nama Penerima (hotel side)
    deliverer_name  VARCHAR(100) NOT NULL,   -- Nama Pengantar (Lolita driver)
    photo_url       TEXT,                    -- R2 object key for delivery photo
    notes           TEXT
);

-- ── Per-order invoices (generated at delivery for every client) ──
CREATE TABLE order_invoices (
    id              BIGSERIAL    PRIMARY KEY,
    invoice_number  VARCHAR(30)  NOT NULL UNIQUE,  -- e.g. INV-PBS-20260601-001
    order_id        BIGINT       NOT NULL UNIQUE REFERENCES orders(id),
    client_id       BIGINT       NOT NULL REFERENCES clients(id),
    invoice_date    DATE         NOT NULL DEFAULT CURRENT_DATE,
    subtotal        NUMERIC(14, 2) NOT NULL,
    pdf_url         TEXT,                          -- R2 object key
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Monthly billing (one per client per month; PBS: one per department per month) ──
CREATE TABLE monthly_billings (
    id              BIGSERIAL    PRIMARY KEY,
    billing_number  VARCHAR(30)  NOT NULL UNIQUE,  -- e.g. BILL-PBS-202606-BL (BL=Bed Linen)
    client_id       BIGINT       NOT NULL REFERENCES clients(id),
    department_id   BIGINT       REFERENCES departments(id),
    period_year     SMALLINT     NOT NULL,
    period_month    SMALLINT     NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    invoice_date    DATE         NOT NULL DEFAULT CURRENT_DATE,
    total           NUMERIC(16, 2) NOT NULL,
    status          VARCHAR(6)   NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT','ISSUED','PAID')),
    pdf_url         TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (client_id, department_id, period_year, period_month)
);

-- ── Monthly billing lines (each delivered order in the billing) ──
CREATE TABLE monthly_billing_lines (
    id              BIGSERIAL       PRIMARY KEY,
    billing_id      BIGINT          NOT NULL REFERENCES monthly_billings(id),
    order_id        BIGINT          NOT NULL REFERENCES orders(id),
    order_number    VARCHAR(30)     NOT NULL,   -- denormalized for display
    order_date      DATE            NOT NULL,   -- denormalized for display
    subtotal        NUMERIC(14, 2)  NOT NULL,
    UNIQUE (billing_id, order_id)
);

-- ── Seed: enable pgcrypto for gen_random_uuid() if not available ──
-- gen_random_uuid() is built-in since PostgreSQL 13; no extension needed.
