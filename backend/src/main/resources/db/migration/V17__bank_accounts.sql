-- ============================================================
-- Multiple company bank accounts + per-client assignment.
--
-- V10 gave the company profile a single bank account, printed on every monthly
-- billing INVOICE. The business now runs two accounts — a personal one and a
-- company one — and large clients (PBS) must be billed to the company account
-- while everyone else keeps the personal one.
--
-- The account list moves out of the company_profile singleton into its own
-- table, and each client may point at one. A NULL clients.bank_account_id means
-- "use the default account", so the seven clients that never change keep working
-- with no configuration at all.
--
-- History rule is unchanged and needs no new columns: monthly_billings already
-- carries its own bank_* snapshot, frozen on the DRAFT -> ISSUED edge (V10), so
-- reassigning a client's account never rewrites an invoice already sent. Only
-- DRAFT billings re-resolve. The order invoice has no bank block at all.
--
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

CREATE TABLE bank_accounts (
    id             BIGSERIAL    PRIMARY KEY,
    label          VARCHAR(50)  NOT NULL,
    beneficiary    VARCHAR(100) NOT NULL,
    bank_name      VARCHAR(50)  NOT NULL,
    account_number VARCHAR(50)  NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order     INT          NOT NULL DEFAULT 0
);

-- At most one default account. Partial index: only rows with is_default = TRUE
-- participate, so any number of non-default accounts is fine.
CREATE UNIQUE INDEX uq_bank_accounts_default ON bank_accounts (is_default) WHERE is_default;

-- Carry the existing single account over as the default (personal) account.
-- The company account is added through the app afterwards, so no real bank
-- details are committed to the repository.
INSERT INTO bank_accounts (label, beneficiary, bank_name, account_number, account_holder,
                           is_default, active, sort_order)
SELECT 'Rekening Pribadi', bank_beneficiary, bank_name, bank_account, bank_holder, TRUE, TRUE, 0
FROM company_profile
WHERE id = 1;

-- The profile keeps the letterhead only; bank_accounts is now the single source
-- of truth for transfer details.
ALTER TABLE company_profile
    DROP COLUMN bank_beneficiary,
    DROP COLUMN bank_name,
    DROP COLUMN bank_account,
    DROP COLUMN bank_holder;

-- NULL = bill this client to the default account.
ALTER TABLE clients ADD COLUMN bank_account_id BIGINT
    CONSTRAINT fk_clients_bank_account REFERENCES bank_accounts (id);
