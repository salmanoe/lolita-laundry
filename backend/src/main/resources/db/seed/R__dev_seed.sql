-- ============================================================
-- DEV-ONLY seed data — gives the UI something to show in Phase 1.
-- Loaded only under the 'dev' / 'authdev' profiles (spring.flyway.locations adds
-- classpath:db/seed). Production never sees it.
-- Repeatable migration (R__) + ON CONFLICT DO NOTHING = safe to re-run.
-- References reference data (units/types) by code via the lookup tables seeded in V2.
-- Item categories were removed in V9 — items have no category; grouping is the per-client
-- department mapping (client_item_departments), set in Atur Harga.
-- ============================================================

-- ── Item master (subset of the real catalogue) ──
INSERT INTO item_master (name, unit_id)
SELECT v.name, u.id
FROM (VALUES
    ('Sheet King',        'PCS'),
    ('Sheet Queen',       'PCS'),
    ('Sheet Single',      'PCS'),
    ('Duvet King',        'PCS'),
    ('Duvet Single',      'PCS'),
    ('Inner Duvet King',  'PCS'),
    ('Bed Cover',         'PCS'),
    ('Pillow Case',       'PCS'),
    ('Bed Runner',        'PCS'),
    ('Blanket King',      'PCS'),
    ('Bath Towel',        'PCS'),
    ('Pool Towel',        'PCS'),
    ('Hand Towel',        'PCS'),
    ('Face Towel',        'PCS'),
    ('Bath Mat',          'PCS'),
    ('Bath Robe',         'PCS'),
    ('Table Cloth',       'PCS'),
    ('Napkin',            'PCS'),
    ('Table Runner',      'PCS'),
    ('Chair Cover',       'PCS'),
    ('FB Apron',          'PCS'),
    ('T-Shirt',           'PCS'),
    ('Vest',              'PCS'),
    ('Jas/Blazer/Jacket', 'PCS'),
    ('Skirt',             'PCS'),
    ('Dress',             'PCS'),
    ('Shirt/Blouse',      'PCS'),
    ('Trouser/Slack',     'PCS'),
    ('Duster',            'PCS'),
    ('Mop Cover',         'PCS'),
    ('Curtain/Black Out', 'M2'),
    ('Vitrage',           'M2'),
    ('Pillow',            'PCS')
) AS v(name, unit_code)
JOIN item_units u ON u.code = v.unit_code
ON CONFLICT (name) DO NOTHING;

-- ── Clients (the 8 real clients; PBS bills per-department) ──
INSERT INTO clients (name, client_code, client_type_id, billing_mode, contact_person, phone)
SELECT v.name, v.code, t.id, v.billing_mode, v.contact, v.phone
FROM (VALUES
    ('Are You and I Hotel',     'AYI', 'HOTEL', 'COMBINED',       'Reception',    '022-0000001'),
    ('Andalucia Hotel',         'AND', 'HOTEL', 'COMBINED',       'Reception',    '022-0000002'),
    ('Doorman Guesthouse',      'DOR', 'HOTEL', 'COMBINED',       'Front Desk',   '022-0000003'),
    ('Hotel Cipaganti 3',       'CIP', 'HOTEL', 'COMBINED',       'Reception',    '022-0000004'),
    ('Ciwulan 36 Guesthouse',   'CIW', 'HOTEL', 'COMBINED',       'Front Desk',   '022-0000005'),
    ('Pasar Baru Square Hotel', 'PBS', 'HOTEL', 'PER_DEPARTMENT', 'Housekeeping', '022-0000006'),
    ('Frances Hotel',           'FRA', 'HOTEL', 'COMBINED',       'Reception',    '022-0000007'),
    ('Gua Bandung Resort',      'GUA', 'HOTEL', 'COMBINED',       'Front Desk',   '022-0000008')
) AS v(name, code, type_code, billing_mode, contact, phone)
JOIN client_types t ON t.code = v.type_code
ON CONFLICT (client_code) DO NOTHING;

-- ── Dev users (real Auth0 subs for the dev tenant — used for local JWT testing under the 'authdev' profile) ──
INSERT INTO users (auth0_sub, full_name, role) VALUES
    ('auth0|6a20cc8b4ae1221e278dacaa', 'Salman Manoe',   'OWNER'),
    ('auth0|6a2216426125bcfe9e007abb', 'Staff Lolita',   'STAFF'),
    ('auth0|6a221661719be467f9f13430', 'Joko Pengantar', 'DRIVER')
ON CONFLICT (auth0_sub) DO NOTHING;

-- ── PBS departments (only PBS uses PER_DEPARTMENT billing) ──
INSERT INTO departments (client_id, name)
SELECT id, dept FROM clients
CROSS JOIN (VALUES ('Room Linen'), ('Uniform/Guest Laundry'), ('F&B Linen')) AS d(dept)
WHERE client_code = 'PBS'
ON CONFLICT (client_id, name) DO NOTHING;