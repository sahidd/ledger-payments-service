CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    owner_name  VARCHAR(200) NOT NULL,
    currency    VARCHAR(3) NOT NULL,
    balance     NUMERIC(19, 2) NOT NULL CHECK (balance >= 0),
    status      VARCHAR(20) NOT NULL,
    version     BIGINT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE transfers (
    id               UUID PRIMARY KEY,
    from_account_id  UUID NOT NULL REFERENCES accounts (id),
    to_account_id    UUID NOT NULL REFERENCES accounts (id),
    amount           NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(3) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    reference        VARCHAR(200),
    failure_reason   VARCHAR(500),
    risk_flags       VARCHAR(500) NOT NULL,
    gateway_auth_id  VARCHAR(120),
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT transfers_distinct_accounts CHECK (from_account_id <> to_account_id)
);

CREATE INDEX idx_transfers_from_created ON transfers (from_account_id, created_at);
CREATE INDEX idx_transfers_pair ON transfers (from_account_id, to_account_id, status);

CREATE TABLE journal_entries (
    id           UUID PRIMARY KEY,
    transfer_id  UUID REFERENCES transfers (id),
    account_id   UUID NOT NULL REFERENCES accounts (id),
    direction    VARCHAR(10) NOT NULL,
    amount       NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency     VARCHAR(3) NOT NULL,
    memo         VARCHAR(200) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT journal_direction CHECK (direction IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_journal_account ON journal_entries (account_id);
CREATE INDEX idx_journal_transfer ON journal_entries (transfer_id);

CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    request_hash    VARCHAR(64) NOT NULL,
    response_status INTEGER,
    response_body   VARCHAR(4000),
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(80) NOT NULL,
    payload        VARCHAR(4000) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_id);
