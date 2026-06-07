-- ============================================================
-- DEV-ONLY seed — backfill invoices & monthly billings from existing orders.
-- Loaded only under 'dev' / 'authdev' (spring.flyway.locations adds classpath:db/seed).
-- Production never sees it.
--
-- Why: orders DELIVERED before the billing module existed never fired
-- OrderDeliveredEvent, so they have no order_invoice and appear in no monthly
-- billing. This backfills both from the orders already in the DB so the Billing
-- UI has data. New deliveries are handled by the live event/on-demand flow.
--
-- Repeatable (R__) + NOT EXISTS guards = idempotent and safe to re-run. Runs
-- after R__dev_seed (alphabetical); on a fresh DB with no orders it inserts
-- nothing. PDFs are not produced here (SQL cannot render them) — PDF_url stays
-- NULL. Per-order invoice PDFs are then rendered lazily on first view
-- (CreateOrderInvoiceUseCase.ensurePdfForOrder, hit by GET /api/orders/{id}/invoice),
-- so "Lihat Invoice" works for backfilled orders. Monthly-billing PDFs are NOT
-- lazily generated — "Lihat PDF" stays hidden until the billing is regenerated.
--
-- Numbering mirrors the application exactly:
--   invoice_number = 'INV-' || order_number              (OrderInvoiceService)
--   billing_number = 'BILL-' || code || '-' || YYYYMM
--                    [ || '-' || dept-abbrev ]           (MonthlyBillingService)
--   dept-abbrev    = first letter of each word, upper, max 4  (BillingFormats)
-- Order/billing totals = SUM(order_line_items.subtotal) (already multiplied).
-- ============================================================

-- ── Per-order invoices: one per DELIVERED order lacking one ──
INSERT INTO order_invoices (invoice_number, order_id, client_id, invoice_date, subtotal)
SELECT 'INV-' || o.order_number,
       o.id,
       o.client_id,
       COALESCE(dc.delivered_at::date, o.order_date),   -- date the order was delivered
       ot.total
FROM orders o
JOIN (
    SELECT order_id, SUM(subtotal) AS total
    FROM order_line_items
    GROUP BY order_id
) ot ON ot.order_id = o.id
LEFT JOIN delivery_confirmations dc ON dc.order_id = o.id
WHERE o.status = 'DELIVERED'
  AND NOT EXISTS (SELECT 1 FROM order_invoices oi WHERE oi.order_id = o.id);

-- ── Monthly billings (+ lines): aggregate DELIVERED orders by order date ──
-- COMBINED clients → one billing (department_id NULL); PER_DEPARTMENT (PBS) →
-- one per department. Department is line-level (V9), so an order is split into its
-- per-department portions: one (order, department) row per touched department.
WITH order_totals AS (
    SELECT o.id              AS order_id,
           o.client_id,
           o.order_number,
           o.order_date,
           c.client_code,
           -- department only participates for PER_DEPARTMENT clients (taken from the line item)
           CASE WHEN c.billing_mode = 'PER_DEPARTMENT' THEN li.department_id END AS eff_dept,
           EXTRACT(YEAR  FROM o.order_date)::int AS yr,
           EXTRACT(MONTH FROM o.order_date)::int AS mo,
           SUM(li.subtotal) AS order_total
    FROM orders o
    JOIN clients c            ON c.id = o.client_id
    JOIN order_line_items li  ON li.order_id = o.id
    WHERE o.status = 'DELIVERED'
    GROUP BY o.id, o.client_id, o.order_number, o.order_date, c.client_code, c.billing_mode,
             CASE WHEN c.billing_mode = 'PER_DEPARTMENT' THEN li.department_id END
),
groups AS (
    SELECT client_id, client_code, eff_dept, yr, mo, SUM(order_total) AS total
    FROM order_totals
    GROUP BY client_id, client_code, eff_dept, yr, mo
),
inserted AS (
    INSERT INTO monthly_billings
        (billing_number, client_id, department_id, department_name, period_year, period_month, invoice_date, total, status)
    SELECT
        'BILL-' || g.client_code || '-' || to_char(make_date(g.yr, g.mo, 1), 'YYYYMM')
            || COALESCE(
                 '-' || (SELECT upper(substring(string_agg(substring(w, 1, 1), ''), 1, 4))
                         FROM regexp_split_to_table(d.name, '[^A-Za-z]+') AS parts(w)
                         WHERE w <> ''),
                 ''),
        g.client_id, g.eff_dept, d.name, g.yr, g.mo, CURRENT_DATE, g.total, 'DRAFT'
    FROM groups g
    LEFT JOIN departments d ON d.id = g.eff_dept
    WHERE NOT EXISTS (
        SELECT 1 FROM monthly_billings mb
        WHERE mb.client_id    = g.client_id
          AND mb.department_id IS NOT DISTINCT FROM g.eff_dept   -- NULL-safe (COMBINED)
          AND mb.period_year  = g.yr
          AND mb.period_month = g.mo
    )
    RETURNING id, client_id, department_id, period_year, period_month
)
INSERT INTO monthly_billing_lines (billing_id, order_id, order_number, order_date, subtotal)
SELECT i.id, ot.order_id, ot.order_number, ot.order_date, ot.order_total
FROM inserted i
JOIN order_totals ot
  ON ot.client_id = i.client_id
 AND ot.eff_dept  IS NOT DISTINCT FROM i.department_id
 AND ot.yr        = i.period_year
 AND ot.mo        = i.period_month;