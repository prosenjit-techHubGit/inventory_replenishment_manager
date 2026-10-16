CREATE TABLE idempotency_request (
    idempotency_key       UUID PRIMARY KEY,
    operation             VARCHAR(64) NOT NULL,
    request_hash          VARCHAR(128) NOT NULL,
    status                VARCHAR(32) NOT NULL,
    resource_id           UUID,
    process_instance_key  VARCHAR(64),
    response_json         TEXT,
    last_error            TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
