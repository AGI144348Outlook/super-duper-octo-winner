CREATE SCHEMA IF NOT EXISTS bubble_connect;
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.users (
  id text PRIMARY KEY, created_at timestamptz NOT NULL DEFAULT now()
);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.device_pairings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id text NOT NULL REFERENCES bubble_connect.users,
  code_hash text UNIQUE NOT NULL, credential_hash text UNIQUE, label text, version text,
  created_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL,
  claimed_at timestamptz, approved_at timestamptz
);
-- next
CREATE INDEX IF NOT EXISTS pairings_by_user ON bubble_connect.device_pairings(user_id,expires_at);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.devices (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id text NOT NULL REFERENCES bubble_connect.users,
  pairing_id uuid UNIQUE NOT NULL REFERENCES bubble_connect.device_pairings,
  label text NOT NULL, version text NOT NULL, credential_hash text UNIQUE NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL,
  last_seen timestamptz, revoked_at timestamptz, UNIQUE(user_id,id)
);
-- next
CREATE INDEX IF NOT EXISTS devices_by_user ON bubble_connect.devices(user_id,created_at DESC);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.ai_connections (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id text NOT NULL REFERENCES bubble_connect.users,
  provider text NOT NULL, credential_hash text UNIQUE NOT NULL,
  expires_at timestamptz NOT NULL, last_used_at timestamptz, revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(user_id,id)
);
-- next
CREATE INDEX IF NOT EXISTS ai_by_user ON bubble_connect.ai_connections(user_id,created_at DESC);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.mcp_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id text NOT NULL, connection_id uuid NOT NULL,
  started_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY(user_id,connection_id) REFERENCES bubble_connect.ai_connections(user_id,id)
);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.tool_permissions (
  user_id text NOT NULL, connection_id uuid NOT NULL, tool text NOT NULL,
  PRIMARY KEY(connection_id,tool),
  FOREIGN KEY(user_id,connection_id) REFERENCES bubble_connect.ai_connections(user_id,id)
);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.tool_invocations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id text NOT NULL, connection_id uuid NOT NULL,
  tool text NOT NULL, outcome text NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY(user_id,connection_id) REFERENCES bubble_connect.ai_connections(user_id,id)
);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.activity_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id text NOT NULL REFERENCES bubble_connect.users,
  event text NOT NULL, subject_id uuid, created_at timestamptz NOT NULL DEFAULT now()
);
-- next
CREATE INDEX IF NOT EXISTS activity_by_user ON bubble_connect.activity_logs(user_id,created_at DESC);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.screen_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id text NOT NULL, device_id uuid NOT NULL,
  started_at timestamptz NOT NULL DEFAULT now(), ended_at timestamptz, status text NOT NULL,
  FOREIGN KEY(user_id,device_id) REFERENCES bubble_connect.devices(user_id,id)
);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.voice_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id text NOT NULL, device_id uuid NOT NULL,
  started_at timestamptz NOT NULL DEFAULT now(), ended_at timestamptz, status text NOT NULL,
  FOREIGN KEY(user_id,device_id) REFERENCES bubble_connect.devices(user_id,id)
);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.browser_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id text NOT NULL REFERENCES bubble_connect.users,
  started_at timestamptz NOT NULL DEFAULT now(), ended_at timestamptz, status text NOT NULL
);
-- next
CREATE TABLE IF NOT EXISTS bubble_connect.nexusboard_preferences (
  user_id text PRIMARY KEY REFERENCES bubble_connect.users, layout jsonb NOT NULL DEFAULT '{}'::jsonb
);
