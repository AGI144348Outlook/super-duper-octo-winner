-- NexusPortal device pairing and speech command relay.
-- Stores hashes for pairing capability; never stores the Android bearer token in plaintext.
CREATE TABLE IF NOT EXISTS nexusportal_pairing_requests (
  pairing_id text PRIMARY KEY,
  pairing_code_hash char(64) NOT NULL UNIQUE,
  device_token_hash char(64) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  approved_at timestamptz,
  consumed_at timestamptz,
  CHECK (expires_at > created_at)
);
CREATE INDEX IF NOT EXISTS nexusportal_pairing_expiry_idx
  ON nexusportal_pairing_requests (expires_at);

CREATE TABLE IF NOT EXISTS nexusportal_devices (
  device_id text PRIMARY KEY,
  token_hash char(64) NOT NULL UNIQUE,
  paired_at timestamptz NOT NULL DEFAULT now(),
  revoked_at timestamptz,
  last_seen_at timestamptz,
  active_session_id text,
  voice_consent boolean NOT NULL DEFAULT false,
  session_started_at timestamptz,
  CHECK (device_id = 'primary')
);

CREATE TABLE IF NOT EXISTS nexusportal_device_commands (
  command_id text PRIMARY KEY,
  device_id text NOT NULL REFERENCES nexusportal_devices(device_id),
  session_id text NOT NULL,
  kind text NOT NULL CHECK (kind IN ('speak_text')),
  payload jsonb NOT NULL,
  status text NOT NULL DEFAULT 'queued'
    CHECK (status IN ('queued','claimed','completed','expired','revoked')),
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  claimed_at timestamptz,
  completed_at timestamptz,
  CHECK (expires_at > created_at)
);
CREATE INDEX IF NOT EXISTS nexusportal_commands_poll_idx
  ON nexusportal_device_commands (device_id, status, created_at);

CREATE TABLE IF NOT EXISTS nexusportal_speech_inputs (
  input_id text PRIMARY KEY,
  device_id text NOT NULL REFERENCES nexusportal_devices(device_id),
  session_id text NOT NULL,
  transcript text NOT NULL CHECK (char_length(transcript) BETWEEN 1 AND 2000),
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  CHECK (expires_at > created_at)
);
CREATE INDEX IF NOT EXISTS nexusportal_speech_inputs_poll_idx
  ON nexusportal_speech_inputs (device_id, session_id, consumed_at, created_at);
