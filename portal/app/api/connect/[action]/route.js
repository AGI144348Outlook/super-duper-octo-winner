import { auth } from '../../../../lib/auth.js';
import { RequestError, claim, dashboard, deviceStatus, ownerAction, mcp } from '../../../../lib/core.mjs';

export const runtime = 'nodejs';

const headers = { 'Cache-Control': 'no-store', 'Referrer-Policy': 'no-referrer' };
const json = (value, status=200) => Response.json(value, { status, headers });

async function parse(request) {
  if (!request.headers.get('content-type')?.startsWith('application/json'))
    throw new RequestError(415, 'JSON required.');
  const raw = await request.text();
  if (raw.length > 12000) throw new RequestError(413, 'Request is too large.');
  let parsed;
  try { parsed = JSON.parse(raw); } catch { throw new RequestError(400, 'Invalid JSON.'); }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
    throw new RequestError(400, 'Expected an object.');
  return parsed;
}

async function identity() {
  const result = await auth().getSession();
  if (!result.data?.user?.id) throw new RequestError(401, 'Sign in first.');
  return result.data.user.id;
}

async function dispatch(request, context) {
  const { action } = await context.params;
  try {
    if (action === 'health' && request.method === 'GET') return json({ portal: 'online' });
    if (action === 'dashboard' && request.method === 'GET') return json(await dashboard(await identity()));
    if (request.method !== 'POST') return json({error:'Method unavailable.'}, 405);
    if (action === 'claim') return json(await claim(await parse(request)));
    if (action === 'device') return json(await deviceStatus(request));
    if (action === 'mcp') {
      const origin = request.headers.get('origin');
      if (origin && origin !== new URL(request.url).origin) return json({error:'Origin rejected.'},403);
      const result = await mcp(request, await parse(request));
      return result === null ? new Response(null, { status:202, headers }) : json(result);
    }
    if (action === 'owner') {
      if (request.headers.get('origin') !== new URL(request.url).origin)
        throw new RequestError(403, 'Cross-origin actions are unavailable.');
      return json(await ownerAction(await identity(), await parse(request)));
    }
    return json({error:'Route unavailable.'}, 404);
  } catch (error) {
    return error instanceof RequestError ? json({ error:error.message },error.status)
      : json({ error:'Service temporarily unavailable.' },503);
  }
}

export const GET = dispatch;
export const POST = dispatch;
