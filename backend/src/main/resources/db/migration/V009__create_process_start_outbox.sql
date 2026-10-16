CREATE TABLE process_start_outbox (
    id                  UUID PRIMARY KEY,
    po_request_id       VARCHAR(80) NOT NULL,
    purchase_order_id   UUID NOT NULL REFERENCES purchase_order (id),
    message_name        VARCHAR(100) NOT NULL,
    correlation_key      VARCHAR(80) NOT NULL,
    payload_json        JSONB NOT NULL,
    status              VARCHAR(32) NOT NULL,
    attempt_count       INTEGER NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ,
    published_at        TIMESTAMPTZ,
    last_error          TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_outbox_po_request_id ON process_start_outbox (po_request_id);
CREATE UNIQUE INDEX uq_outbox_purchase_order_id ON process_start_outbox (purchase_order_id);
CREATE INDEX idx_outbox_status ON process_start_outbox (status);
