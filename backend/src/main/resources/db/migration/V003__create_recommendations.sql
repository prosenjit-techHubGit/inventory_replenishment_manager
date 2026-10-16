CREATE TABLE recommendation (
    id                           UUID PRIMARY KEY,
    item_id                      UUID NOT NULL REFERENCES inventory_item (id),
    current_stock                NUMERIC(14, 2) NOT NULL,
    recommended_quantity         NUMERIC(14, 2) NOT NULL,
    supplier_id                  UUID NOT NULL REFERENCES supplier (id),
    target_delivery_date         DATE NOT NULL,
    risk_level                   VARCHAR(16) NOT NULL,
    rationale                    TEXT,
    status                       VARCHAR(32) NOT NULL,
    camunda_process_instance_key VARCHAR(64),
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at                  TIMESTAMPTZ
);

CREATE INDEX idx_recommendation_status ON recommendation (status);
