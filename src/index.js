/**
 * ECHO Governor Indexing Algorithm — Cloudflare Worker
 * Mashet/LHEA Research · Timothy Marvin Jr. (Quixpydr / Maven)
 *
 * Routes:
 *   GET /agent/{word}           → D1 agents table
 *   GET /axiom/{id}             → ECHO_VGM KV
 *   GET /algorithm/{id}         → ECHO_MATRIX KV
 *   GET /theorem/{id}           → ECHO_LRM KV
 *   GET /state/{key}            → ECHO_STATE KV
 *   GET /geosensory/{key}       → D1 geosensory_endpoints table
 *   GET /formula/{id}           → D1 formulas table
 *   GET /ties/{word}            → D1 ties table (all ties for a word)
 *   GET /search?q={term}        → D1 agents LIKE search
 *   GET /health                 → system status and row counts
 */

export default {
  async fetch(request, env) {
    const url    = new URL(request.url);
    const path   = url.pathname;
    const method = request.method;

    const cors = {
      'Access-Control-Allow-Origin':  '*',
      'Access-Control-Allow-Methods': 'GET, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
      'Content-Type':                 'application/json',
    };

    if (method === 'OPTIONS') return new Response(null, { status: 204, headers: cors });
    if (method !== 'GET') return json({ error: 'Method not allowed' }, 405, cors);

    try {
      if (path === '/health') {
        const [agents, formulas, geo] = await Promise.all([
          env.DB.prepare('SELECT COUNT(*) as n FROM agents').first(),
          env.DB.prepare('SELECT COUNT(*) as n FROM formulas').first(),
          env.DB.prepare('SELECT COUNT(*) as n FROM geosensory_endpoints').first(),
        ]);
        const identity = await env.ECHO_STATE.get('identity');
        const lastMig  = await env.ECHO_STATE.get('last_migration');
        return json({
          status: 'ECHO is live',
          governor: 'A-000 · ר Resh · IDENTIFY → VALIDATE → OPEN',
          identity: identity || 'resh',
          last_migration: lastMig,
          d1: { agents: agents?.n ?? 0, formulas: formulas?.n ?? 0, geosensory_endpoints: geo?.n ?? 0 },
        }, 200, cors);
      }

      const agentMatch = path.match(/^\/agent\/(.+)$/);
      if (agentMatch) {
        const word = decodeURIComponent(agentMatch[1]).toLowerCase();
        const agent = await env.DB.prepare('SELECT * FROM agents WHERE word = ?').bind(word).first();
        if (!agent) return json({ error: `Agent '${word}' not found` }, 404, cors);
        const { results: ties } = await env.DB.prepare('SELECT word_b, tie_weight FROM ties WHERE word_a = ? LIMIT 20').bind(word).all();
        return json({ ...agent, ties }, 200, cors);
      }

      const tiesMatch = path.match(/^\/ties\/(.+)$/);
      if (tiesMatch) {
        const word = decodeURIComponent(tiesMatch[1]).toLowerCase();
        const { results } = await env.DB.prepare('SELECT word_b, tie_weight, tie_source FROM ties WHERE word_a = ? ORDER BY tie_weight DESC LIMIT 30').bind(word).all();
        return json({ word, ties: results }, 200, cors);
      }

      const axiomMatch = path.match(/^\/axiom\/(.+)$/);
      if (axiomMatch) {
        const id = decodeURIComponent(axiomMatch[1]);
        const key = id.startsWith('axiom:') ? id : `axiom:${id}`;
        const val = await env.ECHO_VGM.get(key);
        if (!val) return json({ error: `Axiom '${id}' not found` }, 404, cors);
        return json(JSON.parse(val), 200, cors);
      }

      const algMatch = path.match(/^\/algorithm\/(.+)$/);
      if (algMatch) {
        const id = decodeURIComponent(algMatch[1]).toUpperCase();
        const key = id.startsWith('algorithm:') ? id : `algorithm:${id}`;
        const val = await env.ECHO_MATRIX.get(key);
        if (!val) return json({ error: `Algorithm '${id}' not found` }, 404, cors);
        return json(JSON.parse(val), 200, cors);
      }

      const theoremMatch = path.match(/^\/theorem\/(.+)$/);
      if (theoremMatch) {
        const id = decodeURIComponent(theoremMatch[1]);
        const key = id.startsWith('theorem:') ? id : `theorem:${id}`;
        const val = await env.ECHO_LRM.get(key);
        if (!val) return json({ error: `Theorem '${id}' not found` }, 404, cors);
        return json(JSON.parse(val), 200, cors);
      }

      const stateMatch = path.match(/^\/state\/(.+)$/);
      if (stateMatch) {
        const key = decodeURIComponent(stateMatch[1]);
        const val = await env.ECHO_STATE.get(key);
        if (val === null) return json({ error: `State key '${key}' not found` }, 404, cors);
        return json({ key, value: val }, 200, cors);
      }

      const geoMatch = path.match(/^\/geosensory\/(.+)$/);
      if (geoMatch) {
        const key = decodeURIComponent(geoMatch[1]);
        const row = await env.DB.prepare('SELECT * FROM geosensory_endpoints WHERE key = ?').bind(key).first();
        if (!row) return json({ error: `Geosensory endpoint '${key}' not found` }, 404, cors);
        return json(row, 200, cors);
      }

      const formulaMatch = path.match(/^\/formula\/(.+)$/);
      if (formulaMatch) {
        const id = decodeURIComponent(formulaMatch[1]);
        const row = await env.DB.prepare('SELECT * FROM formulas WHERE alg_id = ?').bind(id).first();
        if (!row) return json({ error: `Formula '${id}' not found` }, 404, cors);
        return json(row, 200, cors);
      }

      if (path === '/search') {
        const q = url.searchParams.get('q');
        if (!q) return json({ error: 'Missing query parameter: q' }, 400, cors);
        const term = `%${q.toLowerCase()}%`;
        const { results } = await env.DB.prepare('SELECT word, department, generality_score FROM agents WHERE word LIKE ? ORDER BY generality_score DESC LIMIT 20').bind(term).all();
        return json({ query: q, results }, 200, cors);
      }

      return json({ error: 'Not found', routes: [
        'GET /health','GET /agent/{word}','GET /ties/{word}','GET /axiom/{id}',
        'GET /algorithm/{id}','GET /theorem/{id}','GET /state/{key}',
        'GET /geosensory/{key}','GET /formula/{id}','GET /search?q={term}',
      ]}, 404, cors);
    } catch (err) {
      return json({ error: 'Internal error', detail: err.message }, 500, cors);
    }
  },
};

function json(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  });
}
