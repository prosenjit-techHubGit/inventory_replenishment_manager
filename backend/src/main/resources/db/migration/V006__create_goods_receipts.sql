CREATE TABLE goods_receipt (
    id                  UUID PRIMARY KEY,
    purchase_order_id   UUID NOT NULL REFERENCES purchase_order (id),
    shipment_id         UUID REFERENCES shipment (id),
    receipt_number      INTEGER NOT NULL,
    received_quantity   NUMERIC(14, 2) NOT NULL,
    accepted_quantity   NUMERIC(14, 2) NOT NULL,
    rejected_quantity   NUMERIC(14, 2) NOT NULL,
    receipt_date        DATE NOT NULL,
    notes               TEXT,
    applied_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (purchase_order_id, receipt_number),
    CHECK (received_quantity > 0),
    CHECK (accepted_quantity >= 0),
    CHECK (rejected_quantity >= 0),
    CHECK (accepted_quantity + rejected_quantity <= received_quantity)
);

CREATE INDEX idx_goods_receipt_purchase_order_id ON goods_receipt (purchase_order_id);
