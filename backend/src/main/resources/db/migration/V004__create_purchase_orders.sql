CREATE SEQUENCE po_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE purchase_order (
    id                            UUID PRIMARY KEY,
    po_number                     VARCHAR(32) NOT NULL UNIQUE,
    po_revision                   INTEGER NOT NULL DEFAULT 1,
    initiation_type               VARCHAR(16) NOT NULL,
    recommendation_id             UUID REFERENCES recommendation (id),
    item_id                       UUID NOT NULL REFERENCES inventory_item (id),
    supplier_id                   UUID NOT NULL REFERENCES supplier (id),
    quantity                      NUMERIC(14, 2) NOT NULL,
    unit_price                    NUMERIC(14, 2) NOT NULL,
    total_value                   NUMERIC(16, 2) NOT NULL,
    required_delivery_date        DATE NOT NULL,
    urgency                       VARCHAR(16) NOT NULL,
    supplier_risk                 VARCHAR(16) NOT NULL,
    business_justification        TEXT,
    status                        VARCHAR(40) NOT NULL,
    po_request_id                 VARCHAR(80) NOT NULL,
    camunda_process_instance_key  VARCHAR(64),
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_purchase_order_recommendation
    ON purchase_order (recommendation_id)
    WHERE recommendation_id IS NOT NULL;

CREATE UNIQUE INDEX uq_purchase_order_po_request_id ON purchase_order (po_request_id);
CREATE INDEX idx_purchase_order_status ON purchase_order (status);
CREATE INDEX idx_purchase_order_process_instance_key ON purchase_order (camunda_process_instance_key);

CREATE TABLE purchase_order_approval (
    id                  UUID PRIMARY KEY,
    purchase_order_id   UUID NOT NULL REFERENCES purchase_order (id),
    po_revision         INTEGER NOT NULL,
    approval_role       VARCHAR(32) NOT NULL,
    approver_user_id    VARCHAR(100),
    outcome             VARCHAR(32),
    comment             TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,
    UNIQUE (purchase_order_id, po_revision, approval_role)
);
