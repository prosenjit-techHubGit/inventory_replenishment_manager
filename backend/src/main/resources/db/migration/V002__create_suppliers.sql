CREATE TABLE supplier (
    id                UUID PRIMARY KEY,
    code              VARCHAR(64) NOT NULL UNIQUE,
    name              VARCHAR(255) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    unit_price        NUMERIC(14, 2) NOT NULL,
    lead_time_days    INTEGER NOT NULL,
    delivery_score    NUMERIC(5, 2),
    quality_score     NUMERIC(5, 2),
    risk_level        VARCHAR(16) NOT NULL DEFAULT 'LOW'
);
