import assert from 'node:assert/strict';
import { createHash, randomBytes } from 'node:crypto';

const origin = process.argv[2];
if (!origin || new URL(origin).origin !== origin) throw new Error('Supply an exact portal origin.');
const password = () => randomBytes(18).toString('hex') + '!A7';
const random = () => randomBytes(32).toString('hex');
const fixture = randomBytes(6).toString('hex');
const path = '/api/connect/';

function client() {
  const jar = new Map();
  return async function request(route, { method='GET', data, token, expected=200, requestOrigin=origin } = {}) {
    const headers = { accept:'application/json', origin:requestOrigin };
    if (data !== undefined) headers['content-type']='application/json';
    if (jar.size) headers.cookie=[...jar].map(([k,v])=>k+'='+v).join('; ');
    if (token) headers.authorization='Bearer '+token;
    const res = await fetch(origin+route,{
      method,headers,redirect:'manual',body:data===undefined?undefined:JSON.stringify(data),
      signal:AbortSignal.timeout(22000)
    });
    for (const cookie of res.headers.getSetCookie()) {
      const part=cookie.split(';',1)[0],eq=part.indexOf('=');
      if (eq>0) jar.set(part.slice(0,eq),part.slice(eq+1));
    }
    assert.equal(res.status,expected,method+' '+route+' returned HTTP '+res.status);
    return res.status===202 ? null : res.json();
  };
}
const anonymous = client(), first = client(), second = client();
const post = (fn,route,data,expected=200,token) => fn(route,{method:'POST',data,expected,token});
const owner = (fn,action,id,expected=200) => post(fn,path+'owner',{action,id},expected);
const rpc = (method,params) => ({jsonrpc:'2.0',id:1,method,params});

await anonymous(path+'health');
await anonymous(path+'dashboard',{expected:401});
await post(anonymous,path+'mcp',rpc('tools/list'),401);
for (const [name,request] of [['A',first],['B',second]]) {
  const email='aibubble-fixture-'+fixture+'-'+name.toLowerCase()+'@example.invalid';
  const pass=password();
  await post(request,'/api/auth/sign-up/email',{name:'Synthetic account '+name,email,password:pass});
  await post(request,'/api/auth/sign-out',{});
  await post(request,'/api/auth/sign-in/email',{email,password:pass});
  const session=await request('/api/auth/get-session');
  assert.equal(session.user.email,email);
  const data=await request(path+'dashboard');
  assert.deepEqual(data.devices,[]);
}
console.log('Managed signup, sign-out, sign-in, and protected dashboard verified for two synthetic accounts.');

await first(path+'owner',{method:'POST',data:{action:'start'},expected:403,requestOrigin:'https://other.example'});
const pair=await owner(first,'start');
const credential=random(), competing=random(), label='Synthetic '+fixture;
const claim={code:pair.code,credential,label,version:'0.4-test'};
await post(anonymous,path+'claim',{...claim,code:'ZZZZ-ZZZZ-ZZZZ'},400);
await post(anonymous,path+'claim',claim);
await post(anonymous,path+'claim',{...claim,credential:competing},409);
assert.equal((await post(anonymous,path+'device',undefined,200,credential)).state,'awaiting approval');
assert.equal((await second(path+'dashboard')).pairings.length,0);
await owner(second,'approve',pair.id,409);
const device=await owner(first,'approve',pair.id);
await owner(first,'approve',pair.id,409);
await post(anonymous,path+'claim',claim,409);
assert.equal((await post(anonymous,path+'device',undefined,200,credential)).state,'online');
assert.equal((await first(path+'dashboard')).devices[0].id,device.id);
assert.equal((await second(path+'dashboard')).devices.length,0);
await owner(second,'revokeDevice',device.id,404);

const aiA=await owner(first,'connectAI'), aiB=await owner(second,'connectAI');
assert.equal((await post(anonymous,path+'mcp',rpc('initialize',{protocolVersion:'2025-11-25'}),200,aiA.token)).result.serverInfo.name,'AIBubble Connect');
assert.equal((await post(anonymous,path+'mcp',rpc('tools/list'),200,aiA.token)).result.tools[0].name,'session_status');
const probe=rpc('tools/call',{name:'session_status',arguments:{}});
assert.equal((await post(anonymous,path+'mcp',probe,200,aiA.token)).result.structuredContent.devices[0].label,label);
assert.deepEqual((await post(anonymous,path+'mcp',probe,200,aiB.token)).result.structuredContent.devices,[]);
await owner(second,'revokeAI',aiA.id,404);
await owner(first,'revokeAI',aiA.id);
await owner(second,'revokeAI',aiB.id);
await post(anonymous,path+'mcp',probe,401,aiA.token);
await owner(first,'revokeDevice',device.id);
await post(anonymous,path+'device',undefined,401,credential);
assert.ok((await first(path+'dashboard')).activity.some(record=>record.event==='AI checked device status'));
console.log('Pair approval, replay rejection, account isolation, revocation, and MCP status verified.');
