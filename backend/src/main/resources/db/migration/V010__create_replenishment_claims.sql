CREATE TABLE replenishment_claim (
    item_id      UUID PRIMARY KEY REFERENCES inventory_item (id),
    run_id       VARCHAR(80) NOT NULL,
    status       VARCHAR(16) NOT NULL,
    claimed_at   TIMESTAMPTZ NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL
);
