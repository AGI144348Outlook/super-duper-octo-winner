// NexusPortal MCP transport foundation — no Android delivery yet.
// Fail closed until a server-side secret and authenticated connection are configured.
// This endpoint intentionally offers only a truthful status tool.
const VERSION = "2025-03-26";
const json = (res, status, body) => { res.statusCode=status; res.setHeader("Content-Type","application/json"); res.setHeader("Cache-Control","no-store"); res.end(JSON.stringify(body)); };
const error = (id, code, message) => ({jsonrpc:"2.0",id,error:{code,message}});
const ok = (id, result) => ({jsonrpc:"2.0",id,result});
module.exports = async function handler(req,res) {
  res.setHeader("X-Content-Type-Options","nosniff");
  if(req.method !== "POST") return json(res,405,{error:"method_not_allowed"});
  const secret=process.env.NEXUSPORTAL_MCP_BEARER_TOKEN;
  // Never silently enable anonymous access.
  if(!secret || secret.length<32) return json(res,503,{error:"mcp_not_configured"});
  const header=req.headers.authorization||"";
  const crypto=require("crypto");
  const supplied=header.startsWith("Bearer ") ? header.slice(7) : "";
  const a=Buffer.from(supplied),b=Buffer.from(secret);
  if(a.length!==b.length || !crypto.timingSafeEqual(a,b)) return json(res,401,{error:"unauthorized"});
  const body=typeof req.body==="string" ? (()=>{try{return JSON.parse(req.body)}catch{return null}})() : req.body;
  if(!body || Array.isArray(body) || body.jsonrpc!=="2.0" || typeof body.method!=="string") return json(res,400,error(null,-32600,"Invalid Request"));
  const id=body.id;
  if(id===undefined) return json(res,202,{accepted:true});
  if(body.method==="initialize") return json(res,200,ok(id,{protocolVersion:VERSION,capabilities:{tools:{}},serverInfo:{name:"nexusportal",version:"0.7.0-foundation"}}));
  if(body.method==="ping") return json(res,200,ok(id,{}));
  if(body.method==="tools/list") return json(res,200,ok(id,{tools:[{name:"portal_status",description:"Report MCP bridge implementation status. Does not read Android screen or device.",inputSchema:{type:"object",properties:{},additionalProperties:false}}]}));
  if(body.method==="tools/call") {
    if(body.params?.name!=="portal_status") return json(res,200,error(id,-32602,"Tool not available"));
    return json(res,200,ok(id,{content:[{type:"text",text:JSON.stringify({mcp:"authenticated",android:"not_connected",annotations:"not_available",screenObservation:false,touchControl:false})}]}));
  }
  return json(res,200,error(id,-32601,"Method not found"));
};
