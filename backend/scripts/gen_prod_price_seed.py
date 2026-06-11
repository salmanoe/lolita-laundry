#!/usr/bin/env python3
"""Generate db/prod-seed/R__prod_price_seed.sql from the Unit-Price CSV.

Throwaway generator (run once at go-live data load). Rules — see plan:
  - use only Hotel / Item / Harga columns; drop benchmark + helper columns
  - drop client 'ptq' (test) and blank rows
  - last-occurrence-wins dedup per (client_code, canonical item)
  - normalize client names -> client_code, item names -> canonical item_master.name
Prints the dedup conflict list to stderr for review.
"""
import csv
import sys
import re
from pathlib import Path

CSV_PATH = Path(r"D:\Projects\Lolita Laundry\Lolita Hotel Laundry Service - Order Form (Responses) - Unit Price.csv")
# Sensitive (per-client prices): kept OUT of src/main/resources so CI never bundles it into the JAR,
# and gitignored (see .gitignore). Deployed by SCP'ing it onto the VPS PROD_SEED_DIR; prod Flyway
# reads it via a filesystem: location (see application-prod.yaml).
OUT_PATH = Path(__file__).resolve().parent.parent / "prod-seed/R__prod_price_seed.sql"
EFFECTIVE_DATE = "2025-01-01"

def norm(s: str) -> str:
    return re.sub(r"\s+", " ", s.strip()).casefold()

CLIENTS = {
    "are you and i hotel": "AYI",
    "andalucia hotel": "AND",
    "doorman guesthouse": "DOO",
    "hotel cipaganti tiga": "CIP",
    "hotel cipaganti 3": "CIP",
    "ciwulan 36 guesthouse": "CIW",
    "pasar baru guesthouse": "PBG",
    "frances hotel": "FRA",
    "gua bandung resort": "GBR",
    "pasar baru square hotel": "PBS",
}

ITEMS = {
    "sheet king": "Sheet King", "sheet queen": "Sheet Queen", "sheet single": "Sheet Single",
    "duvet king": "Duvet King", "duvet single": "Duvet Single",
    "inner duvet king": "Inner Duvet King", "inner duvet single": "Inner Duvet Single",
    "bed cover": "Bed Cover", "blanket king": "Blanket King", "blanket single": "Blanket Single",
    "pillow case": "Pillow Case", "pillow case salur": "Pillow Case Salur",
    "pillow protector": "Pillow Protector", "bed runner": "Bed Runner",
    "bath towel": "Bath Towel", "pool towel": "Pool Towel", "hand towel": "Hand Towel",
    "face towel": "Face Towel", "bath sheet": "Bath Sheet", "bath mat": "Bath Mat",
    "bath robe": "Bath Robe", "table cloth": "Table Cloth", "napkin": "Napkin",
    "table runner": "Table Runner", "chair cover": "Chair Cover",
    "duster": "Duster", "loby duster": "Duster",
    "apron / uniform": "Apron/Uniform", "appron": "Apron/Uniform", "apron": "Apron/Uniform",
    "mop cover": "Mop Cover", "mop": "Mop Cover",
    "bed pad single": "Bed Pad Single", "bed pad king": "Bed Pad King",
    "bed skirting": "Bed Skirting", "cuson case": "Cushion Case", "pillow": "Pillow",
    "cover sofa": "Cover Sofa",
    "curtain / black out (per m²)": "Curtain/Black Out",
    "curtain / black out / per m2": "Curtain/Black Out",
    "vitrage (per m²)": "Vitrage", "vitrage per m2": "Vitrage",
    "shirt / blouse": "Shirt/Blouse", "trouser / slack": "Trouser/Slack",
    "short": "Short", "t-shirt": "T-Shirt", "vest": "Vest", "tie": "Tie",
    "hat cook": "Hat Cook", "jas / blazer / jacket": "Jas/Blazer/Jacket",
    "jacket cook": "Jacket Cook", "skirt": "Skirt", "long skirt": "Long Skirt",
    "dress": "Dress", "long dress": "Long Dress",
    "round table cloth": "Round Table Cloth", "table cloth small": "Table Cloth Small",
    "table cloth large": "Table Cloth Large", "shower curtain": "Shower Curtain",
}

M2_ITEMS = {"Curtain/Black Out", "Vitrage"}

def main():
    prices = {}        # (code, canon_item) -> price   (last wins)
    conflicts = []     # (code, item, old, new)
    unmapped_items = set()
    unmapped_clients = set()

    with CSV_PATH.open(encoding="utf-8-sig", newline="") as f:
        for row in csv.reader(f):
            if len(row) < 3:
                continue
            hotel, item, price = row[0].strip(), row[1].strip(), row[2].strip()
            if not hotel or not item or not price:
                continue
            ck = norm(hotel)
            if ck == "ptq":
                continue
            code = CLIENTS.get(ck)
            if not code:
                unmapped_clients.add(hotel)
                continue
            canon = ITEMS.get(norm(item))
            if not canon:
                unmapped_items.add(item)
                continue
            try:
                p = int(round(float(price)))
            except ValueError:
                continue
            key = (code, canon)
            if key in prices and prices[key] != p:
                conflicts.append((code, canon, prices[key], p))
            prices[key] = p

    if unmapped_clients:
        print("!! UNMAPPED CLIENTS:", sorted(unmapped_clients), file=sys.stderr)
    if unmapped_items:
        print("!! UNMAPPED ITEMS:", sorted(unmapped_items), file=sys.stderr)
        sys.exit("Refusing to generate: unmapped items present.")

    items = sorted({canon for (_c, canon) in prices})
    print(f"clients={len(set(c for c,_ in prices))} items={len(items)} price_rows={len(prices)}",
          file=sys.stderr)
    print(f"-- dedup conflicts (last-wins applied), {len(conflicts)} total --", file=sys.stderr)
    for code, item, old, new in sorted(conflicts):
        print(f"   {code:4} {item:22} {old} -> kept {new}", file=sys.stderr)

    # ---- emit SQL ----
    lines = []
    a = lines.append
    a("-- ============================================================")
    a("-- PRODUCTION seed — item catalogue + per-client unit prices.")
    a("-- Loaded only under the 'prod' profile (application-prod.yaml adds")
    a("-- classpath:db/prod-seed to spring.flyway.locations). Dev/test never see it.")
    a("-- Repeatable migration (R__) + ON CONFLICT DO NOTHING + a FIXED effective_date")
    a("-- = safe to re-run (no duplicate price rows).")
    a("-- Source: 'Lolita Hotel Laundry Service - Order Form (Responses) - Unit Price.csv'.")
    a("-- Generated by backend/scripts/gen_prod_price_seed.py — do not hand-edit; regenerate.")
    a("-- Clients referenced by client_code (already in prod); items by name.")
    a("-- Curtain/Black Out + Vitrage use the M2 unit; everything else PCS.")
    a("-- ============================================================")
    a("")
    a("-- ── Item master (full catalogue from the CSV, normalized) ──")
    a("INSERT INTO item_master (name, unit_id)")
    a("SELECT v.name, u.id")
    a("FROM (VALUES")
    for i, name in enumerate(items):
        unit = "M2" if name in M2_ITEMS else "PCS"
        comma = "," if i < len(items) - 1 else ""
        a(f"    ('{name.replace(chr(39), chr(39)*2)}', '{unit}'){comma}")
    a(") AS v(name, unit_code)")
    a("JOIN item_units u ON u.code = v.unit_code")
    a("ON CONFLICT (name) DO NOTHING;")
    a("")
    a("-- ── Client price lists (one row per client+item; last-occurrence-wins from CSV) ──")
    a("INSERT INTO client_price_lists (client_id, item_id, price_per_unit, effective_date)")
    a("SELECT c.id, i.id, v.price, DATE '%s'" % EFFECTIVE_DATE)
    a("FROM (VALUES")
    rows = sorted(prices.items())
    for idx, ((code, item), price) in enumerate(rows):
        comma = "," if idx < len(rows) - 1 else ""
        esc = item.replace("'", "''")
        a(f"    ('{code}', '{esc}', {price}){comma}")
    a(") AS v(client_code, item_name, price)")
    a("JOIN clients c     ON c.client_code = v.client_code")
    a("JOIN item_master i ON i.name = v.item_name")
    a("ON CONFLICT (client_id, item_id, effective_date) DO NOTHING;")
    a("")

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text("\n".join(lines), encoding="utf-8")
    print(f"wrote {OUT_PATH}", file=sys.stderr)

if __name__ == "__main__":
    main()
