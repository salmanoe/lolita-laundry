-- ============================================================
-- Company profile + per-document company snapshots.
--
-- The company letterhead (name/address/phone) and bank-transfer details were
-- hardcoded in the JasperReports adapter. They can change mid-business (a new
-- address, phone, or bank account), so they move into an OWNER-editable
-- singleton row.
--
-- History rule: a monthly billing freezes a snapshot of the profile when it is
-- ISSUED, and an order invoice freezes the letterhead at creation, so changing
-- the profile never rewrites a document a client already paid against. DRAFT
-- billings follow the live profile. The snapshot columns below hold those frozen
-- values; they are denormalized onto each document row (like department_name in
-- V7) so a PDF can be re-rendered on demand without re-reading the profile.
--
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

-- ── Singleton company profile (id is always 1) ──
CREATE TABLE company_profile (
    id               BIGINT       PRIMARY KEY,
    company_name     VARCHAR(100) NOT NULL,
    address          VARCHAR(200) NOT NULL,
    phone            VARCHAR(30)  NOT NULL,
    bank_beneficiary VARCHAR(100) NOT NULL,
    bank_name        VARCHAR(50)  NOT NULL,
    bank_account     VARCHAR(50)  NOT NULL,
    bank_holder      VARCHAR(100) NOT NULL,
    CONSTRAINT company_profile_singleton CHECK (id = 1)
);

INSERT INTO company_profile (id, company_name, address, phone, bank_beneficiary, bank_name, bank_account, bank_holder)
VALUES (1, 'Lolita Laundry', 'Jl. Sukaraja No. 318 Bandung', '082318359775',
        'Alban Valentino Ramatir', 'Bank BCA', '4061792362', 'Lolita Laundry');

-- ── Order-invoice letterhead snapshot (frozen at creation; no bank block) ──
ALTER TABLE order_invoices ADD COLUMN company_name    VARCHAR(100);
ALTER TABLE order_invoices ADD COLUMN company_address VARCHAR(200);
ALTER TABLE order_invoices ADD COLUMN company_phone   VARCHAR(30);

UPDATE order_invoices
SET company_name    = 'Lolita Laundry',
    company_address = 'Jl. Sukaraja No. 318 Bandung',
    company_phone   = '082318359775'
WHERE company_name IS NULL;

-- ── Monthly-billing company snapshot (frozen at ISSUE; includes bank block) ──
ALTER TABLE monthly_billings ADD COLUMN company_name     VARCHAR(100);
ALTER TABLE monthly_billings ADD COLUMN company_address  VARCHAR(200);
ALTER TABLE monthly_billings ADD COLUMN company_phone    VARCHAR(30);
ALTER TABLE monthly_billings ADD COLUMN bank_beneficiary VARCHAR(100);
ALTER TABLE monthly_billings ADD COLUMN bank_name        VARCHAR(50);
ALTER TABLE monthly_billings ADD COLUMN bank_account     VARCHAR(50);
ALTER TABLE monthly_billings ADD COLUMN bank_holder      VARCHAR(100);

UPDATE monthly_billings
SET company_name     = 'Lolita Laundry',
    company_address  = 'Jl. Sukaraja No. 318 Bandung',
    company_phone    = '082318359775',
    bank_beneficiary = 'Alban Valentino Ramatir',
    bank_name        = 'Bank BCA',
    bank_account     = '4061792362',
    bank_holder      = 'Lolita Laundry'
WHERE company_name IS NULL;