-- ============================================================
-- V10 — Transactions
-- ============================================================

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    listing_id UUID REFERENCES listings(id) ON DELETE SET NULL,
    package_id INT REFERENCES listing_packages(id) ON DELETE RESTRICT,
    amount_vnd NUMERIC(18, 0) NOT NULL,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'BANK_TRANSFER',
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_listing ON transactions(listing_id);
