// NexusPortal Android pairing and one-way speech command channel.
// State is durable in Postgres. Missing storage fails closed.
const crypto = require("crypto");
const db = require("../lib/db");

const reply = (res, status, value) => {
  res.statusCode = status;
  res.setHeader("Content-Type", "application/json");
  res.setHeader("Cache-Control", "no-store");
  res.setHeader("X-Content-Type-Options", "nosniff");
  return res.end(JSON.stringify(value));
};
const sha256 = value => crypto.createHash("sha256").update(value).digest("hex");
const parseBody = req => {
  const body = typeof req.body === "string" ? JSON.parse(req.body || "{}") : req.body;
  return body && !Array.isArray(body) && typeof body === "object" ? body : {};
};
const validHex = (v, n=64) => typeof v === "string" && new RegExp("^[a-f0-9]{"+n+"}$","i").test(v);
const tokenHash = req => {
  const auth = req.headers.authorization || "";
  if (!auth.startsWith("Bearer ")) return "";
  const token = auth.slice(7);
  return token.length >= 32 && token.length <= 256 ? sha256(token) : "";
};

module.exports = async function handler(req, res) {
  res.setHeader("X-Content-Type-Options", "nosniff");
  const action = typeof req.query?.action === "string" ? req.query.action : "";
  if (req.method !== "POST") return reply(res, 405, {error:"method_not_allowed"});
  let body;
  try { body = parseBody(req); } catch { return reply(res, 400, {error:"invalid_json"}); }
  try {
    if (action === "pair-start") {
      const {pairingId, codeHash, deviceTokenHash} = body;
      if (!validHex(pairingId) || !validHex(codeHash) || !validHex(deviceTokenHash)) return reply(res,400,{error:"invalid_pairing_request"});
      await db.query("DELETE FROM nexusportal_pairing_requests WHERE expires_at < now()");
      await db.query(
        "INSERT INTO nexusportal_pairing_requests (pairing_id,pairing_code_hash,device_token_hash,expires_at) VALUES ($1,$2,$3,now()+interval '10 minutes')",
        [pairingId,codeHash.toLowerCase(),deviceTokenHash.toLowerCase()]
      );
      return reply(res,200,{ok:true,expiresInSeconds:600});
    }
    const authHash = tokenHash(req);
    if (!authHash) return reply(res,401,{error:"unauthorized"});
    if (action === "pair-status") {
      if (!validHex(body.pairingId)) return reply(res,400,{error:"invalid_pairing_id"});
      const q = await db.query(
        "SELECT (approved_at IS NOT NULL) AS approved, (expires_at > now()) AS pending FROM nexusportal_pairing_requests WHERE pairing_id=$1 AND device_token_hash=$2",
        [body.pairingId,authHash]
      );
      if (!q.rowCount) return reply(res,404,{error:"pairing_not_found_or_expired"});
      if (!q.rows[0].pending) return reply(res,410,{error:"pairing_expired"});
      return reply(res,200,{approved:q.rows[0].approved});
    }
    if (action === "heartbeat") {
      const consent = body.voiceConsent === true;
      const sessionId = consent && validHex(body.sessionId,64) ? body.sessionId.toLowerCase() : null;
      if (consent && !sessionId) return reply(res,400,{error:"invalid_session"});
      const q = await db.query(
        "UPDATE nexusportal_devices SET last_seen_at=now(), active_session_id=$2, voice_consent=$3, session_started_at=CASE WHEN $3 THEN COALESCE(session_started_at,now()) ELSE NULL END WHERE device_id='primary' AND token_hash=$1 AND revoked_at IS NULL RETURNING device_id",
        [authHash,sessionId,consent]
      );
      if (!q.rowCount) return reply(res,401,{error:"device_unpaired_or_revoked"});
      return reply(res,200,{ok:true,connected:true,sessionId});
    }
    if (action === "session-end") {
      const sessionId = validHex(body.sessionId,64) ? body.sessionId.toLowerCase() : "";
      if (!sessionId) return reply(res,400,{error:"invalid_session"});
      const q = await db.query(
        "UPDATE nexusportal_devices SET active_session_id=NULL,voice_consent=false,session_started_at=NULL,last_seen_at=now() WHERE device_id='primary' AND token_hash=$1 AND revoked_at IS NULL AND active_session_id=$2 RETURNING device_id",
        [authHash,sessionId]
      );
      if (!q.rowCount) return reply(res,409,{error:"session_not_active"});
      await db.query(
        "UPDATE nexusportal_device_commands SET status='revoked' WHERE device_id='primary' AND session_id=$1 AND status='queued'",
        [sessionId]
      );
      return reply(res,200,{ok:true,sessionEnded:true});
    }
    if (action === "command-poll") {
      const q = await db.query(
        "WITH active AS (SELECT device_id,active_session_id FROM nexusportal_devices WHERE device_id='primary' AND token_hash=$1 AND revoked_at IS NULL AND voice_consent=true AND last_seen_at>now()-interval '45 seconds'), next AS (SELECT c.command_id FROM nexusportal_device_commands c JOIN active a ON a.device_id=c.device_id AND a.active_session_id=c.session_id WHERE c.status='queued' AND c.expires_at>now() ORDER BY c.created_at FOR UPDATE OF c SKIP LOCKED LIMIT 1), claimed AS (UPDATE nexusportal_device_commands c SET status='claimed',claimed_at=now() FROM next n WHERE c.command_id=n.command_id RETURNING c.command_id,c.session_id,c.kind,c.payload,c.expires_at) SELECT * FROM claimed",
        [authHash]
      );
      return reply(res,200,{command:q.rows[0]||null});
    }
    if (action === "command-ack") {
      const {commandId,sessionId} = body;
      if (typeof commandId!=="string" || !/^[a-f0-9]{32,64}$/i.test(commandId) || !validHex(sessionId,64)) return reply(res,400,{error:"invalid_ack"});
      const q=await db.query(
        "UPDATE nexusportal_device_commands c SET status='completed',completed_at=now() FROM nexusportal_devices d WHERE c.command_id=$1 AND c.session_id=$2 AND c.status='claimed' AND c.device_id=d.device_id AND d.token_hash=$3 AND d.revoked_at IS NULL AND d.active_session_id=c.session_id RETURNING c.command_id",
        [commandId,sessionId.toLowerCase(),authHash]
      );
      if(!q.rowCount) return reply(res,409,{error:"command_expired_or_stale"});
      return reply(res,200,{ok:true});
    }
    return reply(res,404,{error:"not_found"});
  } catch (e) {
    // Don't expose SQL, credentials, pairing codes, or stack traces to clients.
    console.error("NexusPortal device relay failure", e && e.message);
    return reply(res,503,{error:"relay_unavailable"});
  }
};
