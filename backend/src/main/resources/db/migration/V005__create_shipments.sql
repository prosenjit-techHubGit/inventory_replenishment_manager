CREATE TABLE shipment (
    id                            UUID PRIMARY KEY,
    purchase_order_id             UUID NOT NULL REFERENCES purchase_order (id),
    status                        VARCHAR(16) NOT NULL,
    planned_delivery_date         DATE,
    actual_delivery_date          DATE,
    supplier_shipment_reference   VARCHAR(100),
    tracking_reference            VARCHAR(100),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (purchase_order_id, supplier_shipment_reference)
);

CREATE INDEX idx_shipment_purchase_order_id ON shipment (purchase_order_id);
