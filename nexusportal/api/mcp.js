// NexusPortal MCP: authenticated status, explicit phone pairing, and session-bound speech.
const VERSION = "2025-03-26";
const crypto = require("crypto");
const db = require("./lib/db");
const isHexHash = value => typeof value === "string" && /^[a-f0-9]{64}$/i.test(value);
const equal = (a,b) => { const x=Buffer.from(a),y=Buffer.from(b); return x.length===y.length && crypto.timingSafeEqual(x,y); };
function connectors() {
  try {
    const list=JSON.parse(process.env.NEXUSPORTAL_MCP_CONNECTORS_JSON || "[]");
    if(!Array.isArray(list)) return [];
    return list.filter(x=>x && typeof x.name==="string" && x.name.length>0 && isHexHash(x.sha256) && x.enabled===true);
  } catch { return []; }
}
const json=(res,status,body)=>{res.statusCode=status;res.setHeader("Content-Type","application/json");res.setHeader("Cache-Control","no-store");res.setHeader("X-Content-Type-Options","nosniff");res.end(JSON.stringify(body));};
const error=(id,code,message)=>({jsonrpc:"2.0",id,error:{code,message}});
const ok=(id,result)=>({jsonrpc:"2.0",id,result});
const textResult=value=>({content:[{type:"text",text:JSON.stringify(value)}]});
function tool(name,description,properties={},required=[]) {
  return {name,description,inputSchema:{type:"object",properties,required,additionalProperties:false}};
}
const tools=[
  tool("portal_status","Report whether the authenticated relay and paired Android phone are online. Includes the current phone voice-session id only while the user has explicitly enabled a voice session.",{}),
  tool("approve_device_pairing","Approve the one-time 8-digit code currently displayed in NexusPortal on the phone. The phone must initiate pairing first. Do not approve a code the user has not shown you.",{code:{type:"string",pattern:"^[0-9]{8}$"}},["code"]),
  tool("speak_text","Speak a short ChatGPT reply on the paired phone using Android text-to-speech. Requires an active, user-approved NexusPortal voice session. Use a fresh random 32-character hexadecimal requestId for each new utterance and pass the current sessionId from portal_status.",{text:{type:"string",minLength:1,maxLength:4000},sessionId:{type:"string",pattern:"^[a-fA-F0-9]{64}$"},requestId:{type:"string",pattern:"^[a-fA-F0-9]{32}$"}},["text","sessionId","requestId"]),
  tool("get_speech_transcript","Retrieve and consume one transcript that the user explicitly submitted from the Android speech-recognition dialog. Requires the current active voice session; each transcript is returned once and expires after five minutes.",{sessionId:{type:"string",pattern:"^[a-fA-F0-9]{64}$"}},["sessionId"]),
  tool("revoke_device","Revoke the paired Android phone immediately and cancel its queued speech commands. Requires the user to ask to disconnect or revoke.",{})
];
function cleanToolParams(v) { return v && typeof v==="object" && !Array.isArray(v) ? v : {}; }
async function callTool(name,args,canControl) {
  if(name==="portal_status") {
    const q=await db.query("SELECT revoked_at,last_seen_at,active_session_id,voice_consent FROM nexusportal_devices WHERE device_id='primary'");
    const d=q.rows[0];
    const connected=!!d && !d.revoked_at && d.last_seen_at && (Date.now()-new Date(d.last_seen_at).getTime()<45000);
    const active=connected && d.voice_consent===true && typeof d.active_session_id==="string";
    return textResult({mcp:"authenticated",android:connected?"connected":"not_connected",voice:active?"active":"off",sessionId:canControl&&active?d.active_session_id:null,annotations:"not_available",screenObservation:false,touchControl:false});
  }
  if(name!=="portal_status"&&!canControl) return textResult({error:"chatgpt_connector_required"});
  if(name==="approve_device_pairing") {
    const code=typeof args.code==="string"?args.code.trim():"";
    if(!/^[0-9]{8}$/.test(code)) return textResult({approved:false,error:"invalid_pairing_code"});
    const codeHash=crypto.createHash("sha256").update(code).digest("hex");
    const q=await db.query(
      "WITH request AS (SELECT pairing_id,device_token_hash FROM nexusportal_pairing_requests WHERE pairing_code_hash=$1 AND expires_at>now() AND approved_at IS NULL FOR UPDATE), device AS (INSERT INTO nexusportal_devices (device_id,token_hash) SELECT 'primary',r.device_token_hash FROM request r ON CONFLICT (device_id) DO UPDATE SET token_hash=EXCLUDED.token_hash,paired_at=now(),revoked_at=NULL,last_seen_at=NULL,active_session_id=NULL,voice_consent=false,session_started_at=NULL WHERE nexusportal_devices.revoked_at IS NOT NULL RETURNING device_id), approved AS (UPDATE nexusportal_pairing_requests p SET approved_at=now() FROM request r,device d WHERE p.pairing_id=r.pairing_id RETURNING p.pairing_id) SELECT (SELECT count(*) FROM approved) AS approved, (SELECT count(*) FROM nexusportal_devices WHERE device_id='primary' AND revoked_at IS NULL) AS active_device",
      [codeHash]
    );
    if(Number(q.rows[0]?.approved)===1) return textResult({approved:true,message:"Phone pairing approved. Return to NexusPortal and tap Check pairing."});
    if(Number(q.rows[0]?.active_device)>0) return textResult({approved:false,error:"device_already_paired",message:"Revoke the existing device before pairing another phone."});
    return textResult({approved:false,error:"invalid_or_expired_pairing_code"});
  }
  if(name==="speak_text") {
    const message=typeof args.text==="string"?args.text.trim():"";
    const sessionId=typeof args.sessionId==="string"?args.sessionId.toLowerCase():"";
    const requestId=typeof args.requestId==="string"?args.requestId.toLowerCase():"";
    if(!message || message.length>4000 || !isHexHash(sessionId) || !/^[a-f0-9]{32}$/.test(requestId)) return textResult({accepted:false,error:"invalid_speech_request"});
    if(message.split("").some(ch=>{const n=ch.charCodeAt(0);return (n<32&&n!==9&&n!==10&&n!==13)||n===127;})) return textResult({accepted:false,error:"unsupported_text"});
    const q=await db.query(
      "INSERT INTO nexusportal_device_commands (command_id,device_id,session_id,kind,payload,expires_at) SELECT $1,d.device_id,d.active_session_id,'speak_text',jsonb_build_object('text',$3),now()+interval '45 seconds' FROM nexusportal_devices d WHERE d.device_id='primary' AND d.revoked_at IS NULL AND d.voice_consent=true AND d.active_session_id=$2 AND d.last_seen_at>now()-interval '45 seconds' ON CONFLICT (command_id) DO NOTHING RETURNING command_id",
      [requestId,sessionId,message]
    );
    if(!q.rowCount) return textResult({accepted:false,error:"session_disconnected_stale_or_replayed"});
    return textResult({accepted:true,requestId,sessionId,expiresInSeconds:45});
  }
  if(name==="get_speech_transcript") {
    const sessionId=typeof args.sessionId==="string"?args.sessionId.toLowerCase():"";
    if(!isHexHash(sessionId)) return textResult({received:false,error:"invalid_session"});
    const q=await db.query(
      "WITH active AS (SELECT device_id,active_session_id FROM nexusportal_devices WHERE device_id='primary' AND revoked_at IS NULL AND voice_consent=true AND active_session_id=$1 AND last_seen_at>now()-interval '45 seconds'), next AS (SELECT i.input_id FROM nexusportal_speech_inputs i JOIN active a ON a.device_id=i.device_id AND a.active_session_id=i.session_id WHERE i.consumed_at IS NULL AND i.expires_at>now() ORDER BY i.created_at FOR UPDATE OF i SKIP LOCKED LIMIT 1), consumed AS (UPDATE nexusportal_speech_inputs i SET consumed_at=now() FROM next n WHERE i.input_id=n.input_id RETURNING i.input_id,i.transcript) SELECT * FROM consumed",
      [sessionId]
    );
    if(!q.rowCount) return textResult({received:false,error:"no_pending_speech"});
    return textResult({received:true,inputId:q.rows[0].input_id,transcript:q.rows[0].transcript});
  }
  if(name==="revoke_device") {
    const q=await db.query("UPDATE nexusportal_devices SET revoked_at=now(),active_session_id=NULL,voice_consent=false,session_started_at=NULL WHERE device_id='primary' AND revoked_at IS NULL RETURNING device_id");
    await db.query("UPDATE nexusportal_device_commands SET status='revoked' WHERE device_id='primary' AND status IN ('queued','claimed')");
    await db.query("DELETE FROM nexusportal_pairing_requests");
    return textResult({revoked:!!q.rowCount});
  }
  return null;
}
module.exports=async function handler(req,res) {
  res.setHeader("X-Content-Type-Options","nosniff");
  if(req.method!=="POST") return json(res,405,{error:"method_not_allowed"});
  const master=process.env.NEXUSPORTAL_MCP_BEARER_TOKEN||"";
  const active=connectors();
  if(master.length<32 && active.length===0) return json(res,503,{error:"mcp_not_configured"});
  const header=req.headers.authorization||"";
  const requestPath=(req.url||"").split("?")[0];
  const pathMatch=requestPath.match(new RegExp("^/api/mcp/([A-Za-z0-9_-]{32,256})/?$"));
  const pathToken=pathMatch?pathMatch[1]:"";
  const bearer=header.startsWith("Bearer ")?header.slice(7):"";
  const candidate=pathToken||bearer;
  const hash=crypto.createHash("sha256").update(candidate).digest("hex");
  const caller=active.find(entry=>equal(hash,entry.sha256.toLowerCase()));
  const connectorValid=!!caller;
  const canControl=!!caller&&/chatgpt/i.test(caller.name);
  const masterValid=!pathToken&&master.length>=32&&bearer.length>0&&equal(bearer,master);
  if(!connectorValid&&!masterValid) return json(res,401,{error:"unauthorized"});
  const body=typeof req.body==="string"?(()=>{try{return JSON.parse(req.body)}catch{return null}})():req.body;
  if(!body||Array.isArray(body)||body.jsonrpc!=="2.0"||typeof body.method!=="string") return json(res,400,error(null,-32600,"Invalid Request"));
  const id=body.id;
  if(id===undefined){res.statusCode=202;res.setHeader("Cache-Control","no-store");return res.end();}
  if(body.method==="initialize") return json(res,200,ok(id,{protocolVersion:VERSION,capabilities:{tools:{}},serverInfo:{name:"nexusportal",version:"0.7.0-device-voice"}}));
  if(body.method==="ping") return json(res,200,ok(id,{}));
  if(body.method==="tools/list") return json(res,200,ok(id,{tools}));
  if(body.method==="tools/call") {
    const name=body.params?.name;
    const args=cleanToolParams(body.params?.arguments);
    try {
      const result=await callTool(name,args,canControl);
      if(!result) return json(res,200,error(id,-32602,"Tool not available"));
      return json(res,200,ok(id,result));
    } catch(e) {
      console.error("NexusPortal MCP storage failure",e&&e.message);
      return json(res,200,ok(id,textResult({error:"relay_unavailable",android:"not_connected"})));
    }
  }
  return json(res,200,error(id,-32601,"Method not found"));
};
