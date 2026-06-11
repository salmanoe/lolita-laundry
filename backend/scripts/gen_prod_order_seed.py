#!/usr/bin/env python3
"""Generate backend/prod-seed/R__prod_order_seed.sql from the June-2026 order CSVs.

Throwaway generator (run once at go-live data load), sibling of gen_prod_price_seed.py.
It loads the month's real orders so dashboards, reports and monthly billing show June
from day one. Same mechanism as the price seed: a gitignored repeatable Flyway migration
in backend/prod-seed/, SCP'd onto the VPS PROD_SEED_DIR and applied by prod Flyway via the
filesystem: location already wired in application-prod.yaml. No app code, no schema change.

Sources (all in the parent folder, not the repo):
  - 'Order data bulan Juni 2026 - Order hotel non departmen.csv'  (COMBINED clients)
  - 'Order data bulan Juni 2026 - Order Pasar Baru Square Hotel.csv'  (PBS, PER_DEPARTMENT)
Prices come from backend/prod-seed/prod_prices.csv — a gitignored export of the LIVE prod
client_price_lists (client_code,item_name,price). The live catalogue is the source of truth
(it carries PBS-specific items the older price seed lacked), so line items stay consistent
with what the running app would price; price_at_order is a frozen snapshot anyway.

Correctness gate (refuses to emit on failure, like the price script): the computed total of
every order must equal the CSV's stated total (Total Biaya / GRAND TOTAL), and for PBS each
per-department total must equal TOTAL ROOM LINEN / UNIFORM-GUEST LAUNDRY / F&B LINEN. A wrong
item->item_master mapping or a missing price cannot reconcile, so it is caught before deploy.

Emitted SQL is idempotent (NOT EXISTS guards on order_number) and self-contained (literal
prices/subtotals), then a June-scoped copy of the dev billing backfill builds the per-order
invoices and monthly billings (PBS split per department). Orders are seeded DELIVERED.
"""
import csv
import re
import sys
from datetime import datetime
from decimal import Decimal
from pathlib import Path

BACKEND = Path(__file__).resolve().parent.parent          # backend/
SRC = Path(r"D:\Projects\Lolita Laundry")
NONDEPT_CSV = SRC / "Order data bulan Juni 2026 - Order hotel non departmen.csv"
PBS_CSV = SRC / "Order data bulan Juni 2026 - Order Pasar Baru Square Hotel.csv"
PRICES_CSV = BACKEND / "prod-seed" / "prod_prices.csv"
OUT_PATH = BACKEND / "prod-seed" / "R__prod_order_seed.sql"

PERIOD_START = "2026-06-01"
PERIOD_END = "2026-07-01"          # exclusive
TZ = "+07"                          # WIB
IMPORT_NOTE = "Impor data historis Juni 2026"


def norm(s: str) -> str:
    return re.sub(r"\s+", " ", s.strip()).casefold()


def sql_str(s):
    if s is None or s == "":
        return "NULL"
    return "'" + str(s).replace("'", "''") + "'"


def num(n) -> str:
    """Emit an integer-looking Decimal without trailing zeros."""
    d = Decimal(n)
    return str(d.quantize(Decimal(1)) if d == d.to_integral_value() else d)


# CSV 'Nama Hotel' (normalized) -> (client_code, exact prod clients.name)
CLIENTS = {
    "are you and i hotel": ("AYI", "Are You and I Hotel"),
    "andalucia hotel": ("AND", "Andalucia Hotel"),
    "doorman guesthouse": ("DOO", "Doorman Guesthouse"),
    "hotel cipaganti 3": ("CIP", "Hotel Cipaganti 3"),
    "ciwulan 36 guesthouse": ("CIW", "Ciwulan 36 Guesthouse"),
    "pasar baru guesthouse": ("PBG", "Pasar Baru Guesthouse"),
    "frances hotel": ("FRA", "Frances Hotel"),
    "gua bandung resort": ("GBR", "Gua Bandung Resort"),
    "pasar baru square hotel": ("PBS", "Pasar Baru Square Hotel"),
}

# Non-department CSV: item-column header (normalized) -> item_master.name
NONDEPT_ITEMS = {
    "sheet king": "Sheet King", "sheet queen": "Sheet Queen", "sheet single": "Sheet Single",
    "duvet king": "Duvet King", "duvet single": "Duvet Single",
    "inner duvet king": "Inner Duvet King", "inner duvet single": "Inner Duvet Single",
    "bed cover": "Bed Cover", "blanket king": "Blanket King", "blanket single": "Blanket Single",
    "pillow case": "Pillow Case", "bed runner": "Bed Runner",
    "bath towel": "Bath Towel", "pool towel": "Pool Towel", "hand towel": "Hand Towel",
    "face towel": "Face Towel", "bath sheet": "Bath Sheet", "bath mat": "Bath Mat",
    "bath robe": "Bath Robe", "table cloth": "Table Cloth", "napkin": "Napkin",
    "table runner": "Table Runner", "chair cover": "Chair Cover", "duster": "Duster",
    "bed pad single": "Bed Pad Single", "bed pad king": "Bed Pad King",
}

# PBS CSV: item-column header (normalized) -> (item_master.name, department key)
PBS_ROOM, PBS_UNI, PBS_FB = "ROOM", "UNIFORM", "FB"
PBS_ITEMS = {
    "sheet single": ("Sheet Single", PBS_ROOM), "sheet queen": ("Sheet Queen", PBS_ROOM),
    "sheet king": ("Sheet King", PBS_ROOM), "duvet single": ("Duvet Single", PBS_ROOM),
    "duvet king": ("Duvet King", PBS_ROOM), "inner duvet single": ("Inner Duvet Single", PBS_ROOM),
    "inner duvet king": ("Inner Duvet King", PBS_ROOM), "blanket single": ("Blanket Single", PBS_ROOM),
    "blanket king": ("Blanket King", PBS_ROOM),
    "pillow case / bolster case": ("Pillow Case/Bolster Case", PBS_ROOM),
    "cushion case": ("Cushion Case", PBS_ROOM),
    "bed runner single/queen": ("Bed Runner Single/Queen", PBS_ROOM),
    "bed pad single/double": ("Bed Pad Single / Double", PBS_ROOM),
    "pillow": ("Pillow", PBS_ROOM), "bath towel": ("Bath Towel", PBS_ROOM),
    "hand towel": ("Hand Towel", PBS_ROOM), "face towel": ("Face Towel", PBS_ROOM),
    "bath mat": ("Bath Mat", PBS_ROOM), "duster": ("Duster", PBS_ROOM),
    "mop cover": ("Mop Cover", PBS_ROOM),
    "curtain / black out (per m²)": ("Curtain/Black Out", PBS_ROOM),
    "vitrage (per m²)": ("Vitrage", PBS_ROOM),
    "short": ("Short", PBS_UNI), "t-shirt": ("T-Shirt", PBS_UNI), "vest": ("Vest", PBS_UNI),
    "tie": ("Tie", PBS_UNI), "hat cook": ("Hat Cook", PBS_UNI),
    "jas / blazer / jacket": ("Jas/Blazer/Jacket", PBS_UNI), "jacket cook": ("Jacket Cook", PBS_UNI),
    "skirt": ("Skirt", PBS_UNI), "long skirt": ("Long Skirt", PBS_UNI), "dress": ("Dress", PBS_UNI),
    "long dress": ("Long Dress", PBS_UNI), "shirt / blouse": ("Shirt/Blouse", PBS_UNI),
    "trouser / slack": ("Trouser/Slack", PBS_UNI),
    "fb table cloth (square)": ("FB Table Cloth (Square)", PBS_FB),
    "fb table cloth (round)": ("FB Table Cloth (Round)", PBS_FB),
    "fb table runner": ("FB Table Runner", PBS_FB), "napkin": ("Napkin", PBS_FB),
    "fb chair cover": ("FB Chair Cover", PBS_FB), "fb apron": ("FB Apron", PBS_FB),
}
# Gratis / not billable — present in the sheet but excluded (price 0):
PBS_SKIP = {"dust cloth (towel ooo) – gratis", "dust cloth (towel ooo) - gratis"}
# Exact prod departments.name per department key (note the prod typo 'LAUNDY'):
PBS_DEPT_NAME = {PBS_ROOM: "ROOM LINENS", PBS_UNI: "GUEST LAUNDY / UNIFORM", PBS_FB: "F&B LINEN"}


def load_prices():
    """Load (client_code, item_name) -> price from the gitignored prod_prices.csv.

    The file is the live prod client_price_lists export. A few trailing rows override the
    current catalogue with the HISTORICAL price the June sheet actually billed (where the
    catalogue was edited after the fact) — later rows win, so price_at_order freezes the
    amount the client was invoiced. Lines with fewer than 3 fields (comments) are skipped.
    """
    prices = {}
    with PRICES_CSV.open(encoding="utf-8") as f:
        for row in csv.reader(f):
            if len(row) < 3 or row[0].lstrip().startswith("#"):
                continue
            code, item, price = row[0].strip(), row[1].strip(), row[2].strip()
            prices[(code, item)] = Decimal(price)
    return prices


def parse_qty(cell):
    cell = (cell or "").strip().replace(",", ".")
    if not cell:
        return None
    return Decimal(cell)


def parse_money(cell):
    digits = re.sub(r"[^0-9]", "", cell or "")
    return Decimal(digits) if digits else None


def parse_ts(cell):
    return datetime.strptime(cell.strip(), "%m/%d/%Y %H:%M:%S")


class Order:
    __slots__ = ("number", "client_name", "order_date", "due_date", "mult",
                 "submitted_by", "notes", "created_at", "lines")

    def __init__(self):
        self.lines = []   # (item_name, qty:Decimal, price:Decimal, subtotal:Decimal, dept_name|None)


def build_orders():
    prices = load_prices()
    seq = {}            # (code, date) -> running counter
    orders = []
    errors = []

    def next_number(code, d):
        key = (code, d)
        seq[key] = seq.get(key, 0) + 1
        return f"{code}-{d.strftime('%Y%m%d')}-{seq[key]:03d}"

    # ---- COMBINED clients ----
    with NONDEPT_CSV.open(encoding="utf-8-sig", newline="") as f:
        rows = list(csv.DictReader(f))
    # stable per-day sequence by submission time
    rows.sort(key=lambda r: parse_ts(r["Timestamp"]))
    for r in rows:
        meta = {norm(k): (v or "") for k, v in r.items()}
        hotel = meta.get("nama hotel", "")
        ck = norm(hotel)
        if not ck:
            continue
        if ck not in CLIENTS:
            errors.append(f"[non-dept] unmapped hotel: {hotel!r}")
            continue
        code, cname = CLIENTS[ck]
        ts = parse_ts(meta["timestamp"])
        o = Order()
        o.client_name = cname
        o.created_at = ts
        o.order_date = ts.date()
        o.number = next_number(code, o.order_date)
        pickup = meta.get("tanggal & waktu pickup", "").strip()
        o.due_date = parse_ts(pickup).date() if re.match(r"\d", pickup) else None
        o.mult = Decimal(1)
        o.submitted_by = meta.get("nama staff bertugas", "").strip() or "Impor"
        orig = meta.get("id order", "").strip()
        o.notes = f"{IMPORT_NOTE} (ID asal: {orig})" if orig else IMPORT_NOTE
        for k, v in r.items():
            item = NONDEPT_ITEMS.get(norm(k))
            if not item:
                continue
            qty = parse_qty(v)
            if qty is None or qty == 0:
                continue
            price = prices.get((code, item))
            if price is None:
                errors.append(f"[{o.number}] no price for {code}/{item}")
                continue
            if price == 0:
                continue
            o.lines.append((item, qty, price, qty * price * o.mult, None))
        expected = parse_money(meta.get("total biaya", ""))
        got = sum((ln[3] for ln in o.lines), Decimal(0))
        if expected is None:
            errors.append(f"[{o.number}] missing Total Biaya")
        elif got != expected:
            errors.append(f"[{o.number}] total mismatch: computed {got} != CSV {expected}")
        orders.append(o)

    # ---- PBS (PER_DEPARTMENT) ----
    code, cname = "PBS", CLIENTS["pasar baru square hotel"][1]
    with PBS_CSV.open(encoding="utf-8-sig", newline="") as f:
        rows = list(csv.DictReader(f))
    rows.sort(key=lambda r: parse_ts(r["Timestamp"]))
    for r in rows:
        meta = {norm(k): (v or "") for k, v in r.items()}
        if not meta.get("timestamp", "").strip():
            continue
        ts = parse_ts(meta["timestamp"])
        o = Order()
        o.client_name = cname
        o.created_at = ts
        o.order_date = ts.date()
        o.number = next_number(code, o.order_date)
        o.due_date = None
        o.mult = Decimal(2) if norm(meta.get("jenis pengiriman", "")) == "treatment" else Decimal(1)
        o.submitted_by = meta.get("nama staff hotel bertugas", "").strip() or "Impor"
        orig = meta.get("id order", "").strip()
        catatan = meta.get("catatan / pengaduan khusus", "").strip()
        note = f"{IMPORT_NOTE} (ID asal: {orig})" if orig else IMPORT_NOTE
        if catatan:
            note += f" — {catatan}"
        o.notes = note
        dept_totals = {PBS_ROOM: Decimal(0), PBS_UNI: Decimal(0), PBS_FB: Decimal(0)}
        for k, v in r.items():
            nk = norm(k)
            if nk in PBS_SKIP:
                continue
            mapped = PBS_ITEMS.get(nk)
            if not mapped:
                continue
            item, deptkey = mapped
            qty = parse_qty(v)
            if qty is None or qty == 0:
                continue
            price = prices.get((code, item))
            if price is None:
                errors.append(f"[{o.number}] no price for PBS/{item}")
                continue
            if price == 0:
                continue
            sub = qty * price * o.mult
            o.lines.append((item, qty, price, sub, PBS_DEPT_NAME[deptkey]))
            dept_totals[deptkey] += sub
        # per-department + grand reconciliation
        exp = {
            PBS_ROOM: parse_money(meta.get("total room linen", "")),
            PBS_UNI: parse_money(meta.get("total uniform/guest laundry", "")),
            PBS_FB: parse_money(meta.get("total f&b linen", "")),
        }
        for dk, label in ((PBS_ROOM, "ROOM LINEN"), (PBS_UNI, "UNIFORM/GUEST"), (PBS_FB, "F&B")):
            if exp[dk] is not None and dept_totals[dk] != exp[dk]:
                errors.append(f"[{o.number}] {label} mismatch: computed {dept_totals[dk]} != CSV {exp[dk]}")
        grand_exp = parse_money(meta.get("grand total", ""))
        grand_got = sum(dept_totals.values(), Decimal(0))
        if grand_exp is not None and grand_got != grand_exp:
            errors.append(f"[{o.number}] GRAND mismatch: computed {grand_got} != CSV {grand_exp}")
        orders.append(o)

    return orders, errors


# ---------------------------------------------------------------- SQL emission
BILLING_BACKFILL = f"""
-- ── Per-order invoices: one per imported DELIVERED order lacking one ──
INSERT INTO order_invoices (invoice_number, order_id, client_id, invoice_date, subtotal)
SELECT 'INV-' || o.order_number, o.id, o.client_id, o.order_date, ot.total
FROM orders o
JOIN (SELECT order_id, SUM(subtotal) AS total FROM order_line_items GROUP BY order_id) ot
  ON ot.order_id = o.id
WHERE o.status = 'DELIVERED'
  AND o.order_date >= DATE '{PERIOD_START}' AND o.order_date < DATE '{PERIOD_END}'
  AND NOT EXISTS (SELECT 1 FROM order_invoices oi WHERE oi.order_id = o.id);

-- ── Monthly billings (+lines): aggregate imported DELIVERED orders by order date ──
-- COMBINED → one billing (department_id NULL); PBS → one per line-item department (V9 split).
WITH order_totals AS (
    SELECT o.id AS order_id, o.client_id, o.order_number, o.order_date, c.client_code,
           CASE WHEN c.billing_mode = 'PER_DEPARTMENT' THEN li.department_id END AS eff_dept,
           EXTRACT(YEAR FROM o.order_date)::int AS yr,
           EXTRACT(MONTH FROM o.order_date)::int AS mo,
           SUM(li.subtotal) AS order_total
    FROM orders o
    JOIN clients c ON c.id = o.client_id
    JOIN order_line_items li ON li.order_id = o.id
    WHERE o.status = 'DELIVERED'
      AND o.order_date >= DATE '{PERIOD_START}' AND o.order_date < DATE '{PERIOD_END}'
    GROUP BY o.id, o.client_id, o.order_number, o.order_date, c.client_code, c.billing_mode,
             CASE WHEN c.billing_mode = 'PER_DEPARTMENT' THEN li.department_id END
),
groups AS (
    SELECT client_id, client_code, eff_dept, yr, mo, SUM(order_total) AS total
    FROM order_totals GROUP BY client_id, client_code, eff_dept, yr, mo
),
inserted AS (
    INSERT INTO monthly_billings
        (billing_number, client_id, department_id, department_name, period_year, period_month,
         invoice_date, total, status)
    SELECT
        'BILL-' || g.client_code || '-' || to_char(make_date(g.yr, g.mo, 1), 'YYYYMM')
            || COALESCE('-' || (SELECT upper(substring(string_agg(substring(w, 1, 1), ''), 1, 4))
                                FROM regexp_split_to_table(d.name, '[^A-Za-z]+') AS parts(w)
                                WHERE w <> ''), ''),
        g.client_id, g.eff_dept, d.name, g.yr, g.mo, CURRENT_DATE, g.total, 'DRAFT'
    FROM groups g
    LEFT JOIN departments d ON d.id = g.eff_dept
    WHERE NOT EXISTS (
        SELECT 1 FROM monthly_billings mb
        WHERE mb.client_id = g.client_id
          AND mb.department_id IS NOT DISTINCT FROM g.eff_dept
          AND mb.period_year = g.yr AND mb.period_month = g.mo)
    RETURNING id, client_id, department_id, period_year, period_month
)
INSERT INTO monthly_billing_lines (billing_id, order_id, order_number, order_date, subtotal)
SELECT i.id, ot.order_id, ot.order_number, ot.order_date, ot.order_total
FROM inserted i
JOIN order_totals ot
  ON ot.client_id = i.client_id
 AND ot.eff_dept IS NOT DISTINCT FROM i.department_id
 AND ot.yr = i.period_year AND ot.mo = i.period_month;
"""


def emit(orders):
    L = []
    a = L.append
    a("-- ============================================================")
    a("-- PRODUCTION seed — June 2026 order history (orders + invoices + monthly billings).")
    a("-- Loaded only under the 'prod' profile (application-prod.yaml adds the prod-seed")
    a("-- filesystem: location). Dev/test never see it. Generated by")
    a("-- backend/scripts/gen_prod_order_seed.py — do not hand-edit; regenerate.")
    a("-- Source: 'Order data bulan Juni 2026 - Order hotel non departmen.csv' +")
    a("--         'Order data bulan Juni 2026 - Order Pasar Baru Square Hotel.csv'.")
    a("-- Prices = live prod client_price_lists snapshot; order totals reconciled to the CSV.")
    a("-- Idempotent: NOT EXISTS guards on order_number / order_id keep re-runs a no-op.")
    a("-- Orders are seeded DELIVERED; status history records a single import event.")
    a("-- ============================================================")
    a("")

    # ---- orders ----
    a("-- ── Orders ──")
    a("INSERT INTO orders (order_number, client_id, order_date, due_date, status,")
    a("                    pricing_multiplier, submitted_by_name, notes, created_at)")
    a("SELECT v.order_number, c.id, v.order_date::date, v.due_date::date, 'DELIVERED',")
    a("       v.mult::numeric, v.submitted_by, v.notes, v.created_at::timestamptz")
    a("FROM (VALUES")
    rows = []
    for o in orders:
        due = sql_str(o.due_date.isoformat()) if o.due_date else "NULL"
        created = f"{o.created_at.strftime('%Y-%m-%d %H:%M:%S')}{TZ}"
        rows.append("    ({}, {}, {}, {}, {}, {}, {}, {})".format(
            sql_str(o.number), sql_str(o.client_name), sql_str(o.order_date.isoformat()),
            due, num(o.mult), sql_str(o.submitted_by), sql_str(o.notes), sql_str(created)))
    a(",\n".join(rows))
    a(") AS v(order_number, client_name, order_date, due_date, mult, submitted_by, notes, created_at)")
    a("JOIN clients c ON c.name = v.client_name")
    a("WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_number = v.order_number);")
    a("")

    # ---- line items ----
    a("-- ── Order line items (price_at_order frozen; subtotal = qty x price x multiplier) ──")
    a("INSERT INTO order_line_items (order_id, item_id, quantity, price_at_order, subtotal, department_id)")
    a("SELECT o.id, i.id, v.quantity::numeric, v.price::numeric, v.subtotal::numeric, d.id")
    a("FROM (VALUES")
    rows = []
    for o in orders:
        for item, qty, price, sub, dept in o.lines:
            rows.append("    ({}, {}, {}, {}, {}, {})".format(
                sql_str(o.number), sql_str(item), num(qty), num(price), num(sub),
                sql_str(dept) if dept else "NULL"))
    a(",\n".join(rows))
    a(") AS v(order_number, item_name, quantity, price, subtotal, dept_name)")
    a("JOIN orders o ON o.order_number = v.order_number")
    a("JOIN item_master i ON i.name = v.item_name")
    a("LEFT JOIN departments d ON d.client_id = o.client_id AND d.name = v.dept_name")
    a("WHERE NOT EXISTS (SELECT 1 FROM order_line_items oli")
    a("    WHERE oli.order_id = o.id AND oli.item_id = i.id")
    a("      AND oli.department_id IS NOT DISTINCT FROM d.id);")
    a("")

    # ---- status history ----
    a("-- ── Status history: one import event landing at DELIVERED ──")
    a("INSERT INTO order_status_history (order_id, from_status, to_status, changed_by_id, changed_at, notes)")
    a("SELECT o.id, NULL, 'DELIVERED', NULL, o.created_at, {}".format(sql_str(IMPORT_NOTE)))
    a("FROM orders o")
    a("JOIN (VALUES")
    a(",\n".join("    ({})".format(sql_str(o.number)) for o in orders))
    a(") AS v(order_number) ON v.order_number = o.order_number")
    a("WHERE NOT EXISTS (SELECT 1 FROM order_status_history h WHERE h.order_id = o.id);")
    a("")
    a("-- ── Billing backfill (June-scoped copy of R__dev_seed_billing.sql) ──")
    a(BILLING_BACKFILL.strip())
    a("")
    return "\n".join(L)


def main():
    orders, errors = build_orders()
    n_lines = sum(len(o.lines) for o in orders)
    print(f"orders={len(orders)} line_items={n_lines}", file=sys.stderr)
    if errors:
        print(f"!! {len(errors)} problem(s) — refusing to generate:", file=sys.stderr)
        for e in errors:
            print("   " + e, file=sys.stderr)
        sys.exit(1)
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text(emit(orders), encoding="utf-8")
    print(f"wrote {OUT_PATH}", file=sys.stderr)
    print("all order totals reconcile with the CSV ✓", file=sys.stderr)


if __name__ == "__main__":
    main()
