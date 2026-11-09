import { createHash, randomBytes } from 'node:crypto';

export class RequestError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

const fail = (status, message) => { throw new RequestError(status, message); };
const digest = value => createHash('sha256').update(value).digest('hex');
const idPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const tokenPattern = /^[0-9a-f]{64}$/;
const tool = {
  name: 'session_status',
  description: 'Report connectivity of devices belonging to this AIBubble Connect account.',
  inputSchema: { type: 'object', properties: {}, additionalProperties: false }
};

export async function query(statement, params = []) {
  const connection = process.env.DATABASE_URL;
  if (!connection) fail(503, 'Database is unavailable.');
  const response = await fetch('https://' + new URL(connection).hostname + '/sql', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json', 'Neon-Connection-String': connection,
      'Neon-Array-Mode': 'false', 'Neon-Raw-Text-Output': 'false'
    },
    body: JSON.stringify({ query: statement, params }), cache: 'no-store'
  });
  if (!response.ok) fail(503, 'Database operation is unavailable.');
  return (await response.json()).rows;
}

async function ensureUser(owner) {
  await query('insert into bubble_connect.users(id) values($1) on conflict do nothing', [owner]);
}

export async function dashboard(owner) {
  const [devices, pairings, ai, activity] = await Promise.all([
    query(`select id,label,version,last_seen,expires_at,revoked_at,
      case when revoked_at is not null then 'revoked' when expires_at<=now() then 'expired'
      when last_seen>now()-interval '2 minutes' then 'online'
      when last_seen is null then 'paired' else 'disconnected' end as state
      from bubble_connect.devices where user_id=$1 order by created_at desc limit 30`, [owner]),
    query(`select id,label,version,expires_at,claimed_at from bubble_connect.device_pairings
      where user_id=$1 and approved_at is null and expires_at>now() order by created_at desc limit 10`, [owner]),
    query(`select id,provider,last_used_at,expires_at,revoked_at from bubble_connect.ai_connections
      where user_id=$1 order by created_at desc limit 30`, [owner]),
    query(`select event,created_at from bubble_connect.activity_logs
      where user_id=$1 order by created_at desc limit 30`, [owner])
  ]);
  return { devices, pairings, ai, activity };
}

export async function ownerAction(owner, input) {
  const { action, id } = input;
  if (['approve', 'revokeDevice', 'revokeAI'].includes(action) && (typeof id !== 'string' || !idPattern.test(id)))
    fail(400, 'Invalid record ID.');
  if (action === 'start') {
    await ensureUser(owner);
    const code = randomBytes(6).toString('hex').toUpperCase();
    const [pairing] = await query(`with locked as (
      select id from bubble_connect.users where id=$1 for update),
      pending as (insert into bubble_connect.device_pairings(user_id,code_hash,expires_at)
      select id,$2,now()+interval '5 minutes' from locked
      where (select count(*) from bubble_connect.device_pairings where user_id=$1 and created_at>now()-interval '5 minutes') < 5
      returning id,expires_at),
      audit as (insert into bubble_connect.activity_logs(user_id,event,subject_id)
      select $1,'Pairing requested',id from pending)
      select id,expires_at from pending`, [owner, digest(code)]);
    if (!pairing) fail(429, 'Pairing limit reached. Wait five minutes.');
    return { ...pairing, code: code.match(/.{4}/g).join('-') };
  }
  if (action === 'approve') {
    const [device] = await query(`with approved as (
      update bubble_connect.device_pairings set approved_at=now()
      where id=$2 and user_id=$1 and expires_at>now() and claimed_at is not null and approved_at is null
      returning id,user_id,label,version,credential_hash),
      registered as (insert into bubble_connect.devices(user_id,pairing_id,label,version,credential_hash,expires_at)
      select user_id,id,label,version,credential_hash,now()+interval '30 days' from approved returning id),
      audit as (insert into bubble_connect.activity_logs(user_id,event,subject_id)
      select $1,'Device pairing approved',id from registered)
      select id from registered`, [owner, id]);
    if (!device) fail(409, 'Pairing is expired, already used, or unavailable.');
    return device;
  }
  if (action === 'revokeDevice' || action === 'revokeAI') {
    const table = action === 'revokeDevice' ? 'devices' : 'ai_connections';
    const event = action === 'revokeDevice' ? 'Device revoked' : 'AI connection revoked';
    const [record] = await query(`with revoked as (
      update bubble_connect.${table} set revoked_at=now()
      where id=$2 and user_id=$1 and revoked_at is null returning id),
      audit as (insert into bubble_connect.activity_logs(user_id,event,subject_id) select $1,$3,id from revoked)
      select id from revoked`, [owner, id, event]);
    if (!record) fail(404, 'Connection unavailable.');
    return record;
  }
  if (action === 'connectAI') {
    await ensureUser(owner);
    const token = randomBytes(32).toString('hex');
    const [connection] = await query(`with locked as (select id from bubble_connect.users where id=$1 for update),
      connection as (
      insert into bubble_connect.ai_connections(user_id,provider,credential_hash,expires_at)
      select id,'Bearer MCP client',$2,now()+interval '7 days' from locked
      where (select count(*) from bubble_connect.ai_connections
        where user_id=$1 and revoked_at is null and expires_at>now()) < 5 returning id,user_id,expires_at),
      permission as (insert into bubble_connect.tool_permissions(user_id,connection_id,tool)
      select user_id,id,'session_status' from connection),
      audit as (insert into bubble_connect.activity_logs(user_id,event,subject_id)
      select user_id,'AI connection created',id from connection)
      select id,expires_at from connection`, [owner, digest(token)]);
    if (!connection) fail(409, 'Revoke an existing AI connection before creating another.');
    return { ...connection, token };
  }
  fail(400, 'Unknown action.');
}

function bearer(request) {
  const token = /^Bearer ([0-9a-f]{64})$/i.exec(request.headers.get('authorization') ?? '')?.[1];
  if (!token || !tokenPattern.test(token)) fail(401, 'Connection credential required.');
  return digest(token);
}

export async function claim(input) {
  const { code, credential, label, version } = input;
  const normalized = typeof code === 'string' ? code.replaceAll('-', '').toUpperCase() : '';
  if (!/^[A-F0-9]{12}$/.test(normalized) || typeof credential !== 'string' || !tokenPattern.test(credential) ||
    typeof label !== 'string' || label.trim().length < 1 || label.length > 64 ||
    typeof version !== 'string' || !/^[\w.+-]{1,30}$/.test(version))
    fail(400, 'Invalid pairing request.');
  const [row] = await query(`with paired as (
    update bubble_connect.device_pairings set
      credential_hash=$2,label=$3,version=$4,claimed_at=coalesce(claimed_at,now())
    where code_hash=$1 and expires_at>now() and approved_at is null
    and (claimed_at is null or credential_hash=$2)
    and not exists (select 1 from bubble_connect.device_pairings where credential_hash=$2 and code_hash<>$1)
    returning id,expires_at)
    select id,expires_at from paired`, [digest(normalized), digest(credential), label.trim(), version]);
  if (!row) fail(409, 'Pairing code expired or already used.');
  return { ...row, state: 'awaiting approval' };
}

export async function deviceStatus(request) {
  const hash = bearer(request);
  const [row] = await query(`update bubble_connect.devices set last_seen=now()
    where credential_hash=$1 and revoked_at is null and expires_at>now()
    returning id,label,version,last_seen,expires_at`, [hash]);
  if (row) return { ...row, state: 'online' };
  const [pending] = await query(`select id,expires_at from bubble_connect.device_pairings
    where credential_hash=$1 and approved_at is null and expires_at>now()`, [hash]);
  if (pending) return { ...pending, state: 'awaiting approval' };
  fail(401, 'Device credential expired or revoked.');
}

export async function mcp(request, input) {
  const hash = bearer(request);
  if (input.jsonrpc !== '2.0' || typeof input.method !== 'string') fail(400, 'Expected a JSON-RPC request.');
  const [connection] = await query(`select id,user_id from bubble_connect.ai_connections
    where credential_hash=$1 and revoked_at is null and expires_at>now()`, [hash]);
  if (!connection) fail(401, 'AI connection expired or revoked.');
  const reply = result => ({ jsonrpc: '2.0', id: input.id ?? null, result });
  if (input.method === 'initialize') {
    await query(`insert into bubble_connect.mcp_sessions(user_id,connection_id) values($1,$2)`, [connection.user_id,connection.id]);
    const requested = input.params?.protocolVersion;
    const version = ['2025-03-26','2025-06-18','2025-11-25'].includes(requested) ? requested : '2025-11-25';
    return reply({ protocolVersion: version, serverInfo: { name: 'AIBubble Connect', version: '1.0.0' }, capabilities: { tools: {} } });
  }
  if (input.method === 'notifications/initialized') return null;
  if (input.method === 'ping') return reply({});
  if (input.method === 'tools/list') return reply({ tools: [tool] });
  if (input.method === 'tools/call' && input.params?.name === tool.name) {
    const [active] = await query(`with allowed as (
      update bubble_connect.ai_connections c set last_used_at=now()
      where c.id=$1 and c.revoked_at is null and c.expires_at>now() and exists
      (select 1 from bubble_connect.tool_permissions p where p.connection_id=c.id and p.tool='session_status')
      returning c.id,c.user_id),
      invoked as (insert into bubble_connect.tool_invocations(user_id,connection_id,tool,outcome)
      select user_id,id,'session_status','success' from allowed),
      audited as (insert into bubble_connect.activity_logs(user_id,event,subject_id)
      select user_id,'AI checked device status',id from allowed)
      select user_id from allowed`, [connection.id]);
    if (!active) fail(401, 'AI permission is unavailable.');
    const { devices } = await dashboard(active.user_id);
    const data = { devices: devices.map(({label,version,state}) => ({label,version,state})), screen: 'unverified', voice: 'unverified' };
    return reply({ content: [{ type: 'text', text: JSON.stringify(data) }], structuredContent: data });
  }
  return { jsonrpc: '2.0', id: input.id ?? null, error: { code: -32601, message: 'Method or tool unavailable.' } };
}
