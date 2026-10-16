INSERT INTO inventory_item (id, sku, name, on_hand_quantity, reorder_level, average_consumption, open_purchase_quantity, critical_item)
VALUES ('11111111-1111-1111-1111-111111111111', 'ITEM-100', 'Industrial Sensor', 18, 25, 6, 0, TRUE);

INSERT INTO supplier (id, code, name, active, unit_price, lead_time_days, delivery_score, quality_score, risk_level)
VALUES
    ('22222222-2222-2222-2222-222222222222', 'SUP-001', 'Supplier A', TRUE, 125.50, 7, 95.0, 98.0, 'LOW'),
    ('33333333-3333-3333-3333-333333333333', 'SUP-002', 'Supplier B', TRUE, 118.00, 14, 80.0, 90.0, 'MEDIUM');
