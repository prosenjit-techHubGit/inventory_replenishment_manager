-- Synthetic catalog and historical POs for UI lists/dashboard.
-- Idempotent: safe if V008 demo rows already exist, and safe to re-apply.

INSERT INTO inventory_item (id, sku, name, on_hand_quantity, reorder_level, average_consumption, open_purchase_quantity, critical_item)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000101', 'ITEM-101', 'Hydraulic Actuator', 8, 20, 4, 0, FALSE),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000102', 'ITEM-102', 'Conveyor Belt Module', 140, 50, 12, 0, FALSE),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000103', 'ITEM-103', 'PLC Controller', 4, 15, 2, 10, TRUE),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000104', 'ITEM-104', 'Safety Relay', 22, 20, 3, 100, FALSE),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000105', 'ITEM-105', 'Pneumatic Valve', 6, 25, 5, 80, FALSE),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000106', 'ITEM-106', 'Rotary Encoder', 55, 40, 8, 25, FALSE),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000107', 'ITEM-107', 'Servo Motor', 18, 18, 3, 5, TRUE),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000108', 'ITEM-108', 'Filter Cartridge', 3, 40, 10, 0, FALSE),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000109', 'ITEM-109', 'HMI Panel', 11, 10, 1, 0, FALSE),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000110', 'ITEM-110', '24V Power Supply', 2, 12, 2, 15, TRUE)
ON CONFLICT (sku) DO NOTHING;

INSERT INTO supplier (id, code, name, active, unit_price, lead_time_days, delivery_score, quality_score, risk_level)
VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-000000000003', 'SUP-003', 'Apex Components', TRUE, 42.00, 10, 92.0, 94.0, 'LOW'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-000000000004', 'SUP-004', 'Northwind Industrial', TRUE, 310.00, 21, 78.0, 88.0, 'MEDIUM'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-000000000005', 'SUP-005', 'Helix Motion', TRUE, 890.00, 28, 70.0, 82.0, 'HIGH'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-000000000006', 'SUP-006', 'Pacific Fasteners', TRUE, 1.25, 5, 99.0, 97.0, 'LOW'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-000000000007', 'SUP-007', 'Meridian Hydraulics', TRUE, 215.00, 14, 88.0, 91.0, 'LOW'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-000000000008', 'SUP-008', 'Voltara Electronics', TRUE, 640.00, 18, 81.0, 86.0, 'MEDIUM'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-000000000009', 'SUP-009', 'Harbor Spare Parts', FALSE, 55.00, 35, 61.0, 70.0, 'HIGH')
ON CONFLICT (code) DO NOTHING;

INSERT INTO purchase_order (
    id, po_number, po_revision, initiation_type, recommendation_id,
    item_id, supplier_id, quantity, unit_price, total_value, required_delivery_date,
    urgency, supplier_risk, business_justification, status, po_request_id,
    camunda_process_instance_key, created_at, updated_at
)
VALUES
    (
        'cccccccc-cccc-cccc-cccc-000000000101', 'PO-2026-000101', 1, 'MANUAL', NULL,
        'aaaaaaaa-aaaa-aaaa-aaaa-000000000102', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000003',
        20, 42.00, 840.00, DATE '2026-08-02',
        'NORMAL', 'LOW', 'Restock conveyor modules after line 2 rebuild.',
        'COMPLETED', 'SYNTHETIC:PO-2026-000101', NULL,
        TIMESTAMPTZ '2026-07-12 14:05:00+00', TIMESTAMPTZ '2026-08-03 09:12:00+00'
    ),
    (
        'cccccccc-cccc-cccc-cccc-000000000102', 'PO-2026-000102', 1, 'MANUAL', NULL,
        'aaaaaaaa-aaaa-aaaa-aaaa-000000000103', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000008',
        10, 640.00, 6400.00, DATE '2026-11-20',
        'HIGH', 'MEDIUM', 'PLC controllers for packing cell upgrade. Value requires finance approval.',
        'PENDING_APPROVAL', 'SYNTHETIC:PO-2026-000102', NULL,
        TIMESTAMPTZ '2026-10-14 16:40:00+00', TIMESTAMPTZ '2026-10-14 16:40:00+00'
    ),
    (
        'cccccccc-cccc-cccc-cccc-000000000103', 'PO-2026-000103', 1, 'REPLENISHMENT', NULL,
        'aaaaaaaa-aaaa-aaaa-aaaa-000000000105', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000007',
        80, 215.00, 17200.00, DATE '2026-11-05',
        'NORMAL', 'LOW', 'Replenishment of pneumatic valves; shipment in transit.',
        'IN_FULFILLMENT', 'SYNTHETIC:PO-2026-000103', NULL,
        TIMESTAMPTZ '2026-09-28 11:18:00+00', TIMESTAMPTZ '2026-10-10 08:00:00+00'
    ),
    (
        'cccccccc-cccc-cccc-cccc-000000000104', 'PO-2026-000104', 1, 'MANUAL', NULL,
        'aaaaaaaa-aaaa-aaaa-aaaa-000000000107', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000005',
        5, 890.00, 4450.00, DATE '2026-12-01',
        'URGENT', 'HIGH', 'Servo motors for critical spindle. Awaiting supplier confirmation.',
        'PENDING_SUPPLIER_CONFIRMATION', 'SYNTHETIC:PO-2026-000104', NULL,
        TIMESTAMPTZ '2026-10-15 09:22:00+00', TIMESTAMPTZ '2026-10-16 13:05:00+00'
    ),
    (
        'cccccccc-cccc-cccc-cccc-000000000105', 'PO-2026-000105', 1, 'MANUAL', NULL,
        'aaaaaaaa-aaaa-aaaa-aaaa-000000000101', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000004',
        40, 310.00, 12400.00, DATE '2026-09-15',
        'NORMAL', 'MEDIUM', 'Rejected: lead time incompatible with shutdown window.',
        'REJECTED', 'SYNTHETIC:PO-2026-000105', NULL,
        TIMESTAMPTZ '2026-08-20 10:00:00+00', TIMESTAMPTZ '2026-08-22 15:44:00+00'
    ),
    (
        'cccccccc-cccc-cccc-cccc-000000000106', 'PO-2026-000106', 1, 'MANUAL', NULL,
        'aaaaaaaa-aaaa-aaaa-aaaa-000000000109', '22222222-2222-2222-2222-222222222222',
        6, 125.50, 753.00, DATE '2026-10-01',
        'NORMAL', 'LOW', 'HMI panels received in full.',
        'RECEIVED', 'SYNTHETIC:PO-2026-000106', NULL,
        TIMESTAMPTZ '2026-09-02 08:30:00+00', TIMESTAMPTZ '2026-10-02 17:10:00+00'
    ),
    (
        'cccccccc-cccc-cccc-cccc-000000000107', 'PO-2026-000107', 1, 'REPLENISHMENT', NULL,
        'aaaaaaaa-aaaa-aaaa-aaaa-000000000104', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000006',
        100, 1.25, 125.00, DATE '2026-11-08',
        'NORMAL', 'LOW', 'Approved fastener restock; release pending.',
        'APPROVED', 'SYNTHETIC:PO-2026-000107', NULL,
        TIMESTAMPTZ '2026-10-13 12:00:00+00', TIMESTAMPTZ '2026-10-16 10:15:00+00'
    ),
    (
        'cccccccc-cccc-cccc-cccc-000000000108', 'PO-2026-000108', 1, 'MANUAL', NULL,
        'aaaaaaaa-aaaa-aaaa-aaaa-000000000110', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000008',
        15, 640.00, 9600.00, DATE '2026-10-28',
        'HIGH', 'MEDIUM', 'Partial receipt of 24V supplies; remainder still due.',
        'PARTIALLY_RECEIVED', 'SYNTHETIC:PO-2026-000108', NULL,
        TIMESTAMPTZ '2026-09-18 07:45:00+00', TIMESTAMPTZ '2026-10-12 11:20:00+00'
    ),
    (
        'cccccccc-cccc-cccc-cccc-000000000109', 'PO-2026-000109', 1, 'MANUAL', NULL,
        'aaaaaaaa-aaaa-aaaa-aaaa-000000000106', 'bbbbbbbb-bbbb-bbbb-bbbb-000000000003',
        25, 42.00, 1050.00, DATE '2026-11-12',
        'NORMAL', 'LOW', 'Encoders released to supplier; fulfillment not started.',
        'RELEASED', 'SYNTHETIC:PO-2026-000109', NULL,
        TIMESTAMPTZ '2026-10-11 19:00:00+00', TIMESTAMPTZ '2026-10-16 08:00:00+00'
    )
ON CONFLICT (po_number) DO NOTHING;

SELECT setval('po_number_seq', GREATEST((SELECT last_value FROM po_number_seq), 200));
