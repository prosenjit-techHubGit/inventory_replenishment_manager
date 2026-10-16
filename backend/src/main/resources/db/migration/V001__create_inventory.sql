CREATE TABLE inventory_item (
    id                      UUID PRIMARY KEY,
    sku                     VARCHAR(64) NOT NULL UNIQUE,
    name                    VARCHAR(255) NOT NULL,
    on_hand_quantity        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    reorder_level           NUMERIC(14, 2) NOT NULL DEFAULT 0,
    average_consumption     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    open_purchase_quantity  NUMERIC(14, 2) NOT NULL DEFAULT 0,
    critical_item           BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 0
);
