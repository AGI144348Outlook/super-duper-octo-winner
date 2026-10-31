// NexusPortal MCP transport foundation — no Android delivery yet.
// Fail closed until a server-side secret and authenticated connection are configured.
// This endpoint intentionally offers only a truthful status tool.
const VERSION = "2025-03-26";
const crypto = require("crypto");
const isHexHash = value => typeof value === "string" && /^[a-f0-9]{64}$/i.test(value);
const equal = (a,b) => { const x=Buffer.from(a),y=Buffer.from(b); return x.length===y.length && crypto.timingSafeEqual(x,y); };
// Independent, revocable connectors: NEXUSPORTAL_MCP_CONNECTORS_JSON
// Example: [{"name":"chatgpt","sha256":"<64 hex digits>","enabled":true}]
// Never place raw capability keys in this variable or the repository.
function connectors() {
  try {
    const list=JSON.parse(process.env.NEXUSPORTAL_MCP_CONNECTORS_JSON || "[]");
    if(!Array.isArray(list)) return [];
    return list.filter(x=>x && typeof x.name==="string" && x.name.length>0 && isHexHash(x.sha256) && x.enabled===true);
  } catch { return []; }
}
const json = (res, status, body) => { res.statusCode=status; res.setHeader("Content-Type","application/json"); res.setHeader("Cache-Control","no-store"); res.end(JSON.stringify(body)); };
const error = (id, code, message) => ({jsonrpc:"2.0",id,error:{code,message}});
const ok = (id, result) => ({jsonrpc:"2.0",id,result});
module.exports = async function handler(req,res) {
  res.setHeader("X-Content-Type-Options","nosniff");
  if(req.method !== "POST") return json(res,405,{error:"method_not_allowed"});
  const master=process.env.NEXUSPORTAL_MCP_BEARER_TOKEN || "";
  const active=connectors();
  // No configured credentials means no access, including through capability URLs.
  if(master.length<32 && active.length===0) return json(res,503,{error:"mcp_not_configured"});
  const header=req.headers.authorization||"";
  const requestPath=(req.url||"").split("?")[0];
  const pathMatch=requestPath.match(new RegExp("^/api/mcp/([A-Za-z0-9_-]{32,256})/?$"));
  const pathToken=pathMatch ? pathMatch[1] : "";
  const bearer=header.startsWith("Bearer ") ? header.slice(7) : "";
  const candidate=pathToken || bearer;
  const hash=crypto.createHash("sha256").update(candidate).digest("hex");
  const connectorValid=active.some(entry=>equal(hash,entry.sha256.toLowerCase()));
  // Master is bearer-only, never accepted as a URL capability.
  const masterValid=!pathToken && master.length>=32 && bearer.length>0 && equal(bearer,master);
  if(!connectorValid && !masterValid) return json(res,401,{error:"unauthorized"});
  const body=typeof req.body==="string" ? (()=>{try{return JSON.parse(req.body)}catch{return null}})() : req.body;
  if(!body || Array.isArray(body) || body.jsonrpc!=="2.0" || typeof body.method!=="string") return json(res,400,error(null,-32600,"Invalid Request"));
  const id=body.id;
  if(id===undefined) { res.statusCode=202; res.setHeader("Cache-Control","no-store"); return res.end(); }
  if(body.method==="initialize") return json(res,200,ok(id,{protocolVersion:VERSION,capabilities:{tools:{}},serverInfo:{name:"nexusportal",version:"0.7.0-foundation"}}));
  if(body.method==="ping") return json(res,200,ok(id,{}));
  if(body.method==="tools/list") return json(res,200,ok(id,{tools:[{name:"portal_status",description:"Report MCP bridge implementation status. Does not read Android screen or device.",inputSchema:{type:"object",properties:{},additionalProperties:false}}]}));
  if(body.method==="tools/call") {
    if(body.params?.name!=="portal_status") return json(res,200,error(id,-32602,"Tool not available"));
    return json(res,200,ok(id,{content:[{type:"text",text:JSON.stringify({mcp:"authenticated",android:"not_connected",annotations:"not_available",screenObservation:false,touchControl:false})}]}));
  }
  return json(res,200,error(id,-32601,"Method not found"));
};
