-- Pairing offers and trusted-client credentials for Android trusted reconnect.

-- ── One-time pairing codes ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS pairing_codes (
  code_id             TEXT    PRIMARY KEY,
  code_hash           TEXT    NOT NULL UNIQUE,
  code_display        TEXT    NOT NULL,
  created_at          TEXT    NOT NULL,
  expires_at          TEXT    NOT NULL,
  claimed_at          TEXT,
  claimed_by_client_id TEXT    REFERENCES trusted_clients(client_id)
);

CREATE INDEX IF NOT EXISTS idx_pairing_codes_expires_at ON pairing_codes(expires_at);
CREATE INDEX IF NOT EXISTS idx_pairing_codes_claimed_at  ON pairing_codes(claimed_at);

-- ── Trusted client registry ───────────────────────────────────────

CREATE TABLE IF NOT EXISTS trusted_clients (
  client_id          TEXT    PRIMARY KEY,
  client_secret_hash TEXT    NOT NULL,
  device_label       TEXT,
  created_at         TEXT    NOT NULL,
  updated_at         TEXT    NOT NULL,
  last_seen_at       TEXT,
  revoked_at         TEXT
);

CREATE INDEX IF NOT EXISTS idx_trusted_clients_last_seen_at ON trusted_clients(last_seen_at);
CREATE INDEX IF NOT EXISTS idx_trusted_clients_revoked_at   ON trusted_clients(revoked_at);
