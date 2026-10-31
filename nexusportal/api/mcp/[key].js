// Dynamic Vercel route for revocable, per-connector MCP capability URLs.
// Keep the key in the path; the shared handler verifies it before returning tools.
const handler = require('../mcp.js');
module.exports = (req, res) => {
  const key = typeof req.query?.key === 'string' ? req.query.key : '';
  if (!/^[A-Za-z0-9_-]{32,256}$/.test(key)) {
    res.statusCode = 401;
    res.setHeader('Cache-Control', 'no-store');
    return res.end(JSON.stringify({error:'unauthorized'}));
  }
  // The shared handler extracts the capability from the original route path.
  req.url = '/api/mcp/' + key;
  return handler(req, res);
};
