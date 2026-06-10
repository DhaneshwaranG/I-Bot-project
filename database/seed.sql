USE ibot_db;

-- ── Admin user (password: password123) ────────────────────
INSERT IGNORE INTO users (name, email, password, department, role) VALUES
('Admin User',     'admin@ibot.com',   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCgSWxA0n.mfhA8HgD.rqv6', 'Finance',    'ROLE_ADMIN'),
('Priya Sharma',   'priya@ibot.com',   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCgSWxA0n.mfhA8HgD.rqv6', 'Accounts',   'ROLE_USER'),
('Rahul Verma',    'rahul@ibot.com',   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCgSWxA0n.mfhA8HgD.rqv6', 'Operations', 'ROLE_USER');

-- ── Vendors ────────────────────────────────────────────────
INSERT IGNORE INTO vendors (name, vendor_code, email, gst_number, active) VALUES
('Acme Corporation',        'VND-001', 'billing@acme.com',         '27AABCA1234A1Z5', true),
('Global Logistics Ltd',    'VND-002', 'accounts@globallogistics.in', '29AABCG5678B1Z2', true),
('TechSolutions Pvt Ltd',   'VND-003', 'finance@techsolutions.co',   '07AABCT9012C1Z8', true),
('Office Depot India',      'VND-004', 'invoices@officedepot.in',    '19AABCO3456D1Z4', true),
('Alpha Technologies',      'VND-005', 'ar@alphatech.io',            '27AABCA7890E1Z1', true);

-- ── Sample invoices ────────────────────────────────────────
INSERT INTO invoices (invoice_number, vendor_name, invoice_date, due_date, subtotal, gst_amount, gst_rate, total_amount, currency, status, ocr_confidence, uploaded_by) VALUES
('INV-2024-0001', 'Acme Corporation',      '2024-10-24', '2024-11-24', 1050.00,  189.00, 18.00, 1239.00,  'INR', 'APPROVED',  92.5, 1),
('INV-2024-0002', 'Global Logistics Ltd',  '2024-10-22', '2024-11-22',  720.76,  129.74, 18.00,  850.50,  'INR', 'FLAGGED',   68.3, 1),
('INV-2024-0003', 'TechSolutions Pvt Ltd', '2024-10-21', '2024-12-20', 2627.12,  472.88, 18.00, 3100.00,  'INR', 'PENDING',     NULL, 1),
('INV-2024-0004', 'Office Depot India',    '2024-10-19', '2024-11-19',  132.39,   23.83, 18.00,  156.22,  'INR', 'VALIDATED', 95.1, 2),
('INV-2024-0005', 'Alpha Technologies',    '2024-10-18', '2024-11-17', 2372.88,  427.12, 18.00, 2800.00,  'INR', 'APPROVED',  88.7, 2),
('INV-2024-0006', 'Acme Corporation',      '2024-11-01', '2024-12-01', 3500.00,  630.00, 18.00, 4130.00,  'INR', 'EXTRACTED', 79.2, 3),
('INV-2024-0007', 'TechSolutions Pvt Ltd', '2024-11-05', '2024-12-05', 1800.00,  324.00, 18.00, 2124.00,  'INR', 'APPROVED',  91.4, 1);
