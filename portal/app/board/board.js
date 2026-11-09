'use client';

import { useEffect, useState } from 'react';
import { createAuthClient } from '@neon/auth/next';

const auth = createAuthClient();
const date = value => value ? new Date(value).toLocaleString() : 'No activity yet';

async function api(path, body) {
  const response = await fetch('/api/connect/'+path, body === undefined ? {cache:'no-store'} : {
    method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)
  });
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || 'Request failed.');
  return result;
}

export default function Board() {
  const session = auth.useSession();
  const user = session.data?.user;
  const [mode,setMode] = useState('signIn');
  const [data,setData] = useState(null);
  const [pending,setPending] = useState(false);
  const [notice,setNotice] = useState('');
  const [code,setCode] = useState(null);
  const [token,setToken] = useState(null);

  useEffect(() => {
    setData(null);
    setCode(null);
    setToken(null);
    if (!user?.id) return;
    let active = true;
    async function refresh() {
      try {
        const result = await api('dashboard');
        if (active) setData(result);
      } catch (error) { if (active) setNotice(error.message); }
    }
    refresh();
    const timer = setInterval(refresh, 15000);
    return () => { active = false; clearInterval(timer); };
  },[user?.id]);

  async function sign(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    setNotice('');
    setPending(true);
    try {
      const result = mode === 'signIn'
        ? await auth.signIn.email({email:values.email,password:values.password})
        : await auth.signUp.email({name:values.name,email:values.email,password:values.password});
      if (result.error) throw new Error(result.error.message || 'Sign-in could not be completed.');
      await session.refetch();
      if (mode === 'signUp' && !result.data?.token) setNotice('Account created. Check your email if verification is required.');
    } catch (error) { setNotice(error.message); }
    finally { setPending(false); }
  }

  async function action(name,id) {
    setPending(true);
    setNotice('');
    try {
      const result = await api('owner',{action:name,id});
      if (name === 'start') setCode(result);
      if (name === 'connectAI') setToken(result);
      if (name === 'revokeAI' && token?.id === id) setToken(null);
      setData(await api('dashboard'));
      if (name === 'approve') setCode(null);
    } catch (error) { setNotice(error.message); }
    finally { setPending(false); }
  }

  if (session.isPending) return <p role="status">Checking your sign-in…</p>;
  if (!user) return <section className="panel auth">
    <h2>{mode === 'signIn' ? 'Sign in' : 'Create your account'}</h2>
    <form onSubmit={sign}>
      {mode === 'signUp' && <label>Name<input name="name" autoComplete="name" maxLength={80} required /></label>}
      <label>Email<input name="email" type="email" autoComplete="email" required /></label>
      <label>Password<input name="password" type="password" minLength={8} maxLength={128} autoComplete={mode==='signIn'?'current-password':'new-password'} required /></label>
      <button className="primary" disabled={pending}>{mode === 'signIn' ? 'Sign in' : 'Register'}</button>
    </form>
    <button type="button" onClick={() => {setMode(mode==='signIn'?'signUp':'signIn');setNotice('');}}>{mode === 'signIn' ? 'Create an account' : 'Back to sign in'}</button>
    <p role="status">{notice}</p>
  </section>;

  return <>
    <div className="actions"><span>{user.name}</span><button onClick={async () => {
      try { await auth.signOut(); await session.refetch(); } catch { setNotice('Sign-out could not be confirmed.'); }
    }}>Sign out</button></div>
    <p role="status">{notice || (!data ? 'Loading your workspace…' : '')}</p>
    <div className="grid">
      <section className="panel"><div className="kicker">01 / HARDWARE</div><h2>My Device</h2>
        <p>Generate a five-minute code, enter it in a compatible AIBubble client, then approve the matching device here.</p>
        <button className="primary" disabled={pending} onClick={() => action('start')}>Generate pairing code</button>
        {code && <p className="secret"><strong>{code.code}</strong><br />Expires {date(code.expires_at)}. Enter this code only on your own device.</p>}
        <ul>{data?.pairings.map(p => <li key={p.id}>
          {p.claimed_at ? <><strong>{p.label}</strong> · {p.version}<button disabled={pending} onClick={() => action('approve',p.id)}>Approve matching device</button></>
            : <>Pairing requested · {date(p.expires_at)}</>}
        </li>)}</ul>
        {!data?.devices.length && <p>No paired devices.</p>}
        <ul>{data?.devices.map(d => <li key={d.id}>
          <strong>{d.label}</strong> · {d.version} · {d.state}<br />Last seen {date(d.last_seen)}
          {!d.revoked_at && <button disabled={pending} onClick={() => action('revokeDevice',d.id)}>Revoke device</button>}
        </li>)}</ul>
      </section>
      <section className="panel" id="ai"><div className="kicker">02 / MCP</div><h2>My AI</h2>
        <p>Available tool <code>session_status</code>. Generate a seven-day credential for clients that support a custom HTTP Bearer header.</p>
        <p>Use the <code>/api/connect/mcp</code> path on this portal’s origin as the Streamable HTTP endpoint.</p>
        <button className="primary" disabled={pending} onClick={() => action('connectAI')}>Create MCP credential</button>
        {token && <div className="secret"><p>Shown once. Put it in the client’s Authorization Bearer field.</p><code>{token.token}</code><button onClick={()=>setToken(null)}>Hide credential</button></div>}
        <ul>{data?.ai.map(a => <li key={a.id}>
          <strong>{a.provider}</strong> · {a.revoked_at ? 'revoked' : Date.parse(a.expires_at) <= Date.now() ? 'expired' : a.last_used_at ? 'tool verified' : 'waiting for first call'}<br />
          Last successful tool {date(a.last_used_at)}
          {!a.revoked_at && <button disabled={pending} onClick={() => action('revokeAI',a.id)}>Revoke AI credential</button>}
        </li>)}</ul>
        <details><summary>ChatGPT, Claude and Gemini</summary><p>ChatGPT custom app setup connects MCP tools separately from your ChatGPT sign-in. This endpoint currently supports Bearer-header clients. ChatGPT OAuth discovery and the consumer connection are pending.</p><p>Claude or Gemini clients can use this endpoint only where their MCP client accepts a custom Bearer header. Provider-specific sign-in is pending.</p></details>
      </section>
      <section className="panel"><div className="kicker">03 / PERMISSIONS</div><h2>Sight &amp; Sound</h2>
        <p><strong>ע Sight</strong> and <strong>פ Sound</strong> start on the Android app. Stop sessions on the phone. MediaProjection permission and Accessibility are distinct.</p>
        <p>Screen delivery and audio are unverified in this portal. A device heartbeat confirms connectivity only.</p>
      </section>
      <section className="panel"><div className="kicker">04 / HISTORY</div><h2>Activity</h2>
        {!data?.activity.length && <p>No activity yet.</p>}
        <ul>{data?.activity.map((a,i) => <li key={a.created_at+i}>{a.event}<br /><small>{date(a.created_at)}</small></li>)}</ul>
      </section>
      <section className="panel"><div className="kicker">05 / WORKSPACE</div><h2>NexusBoard Workspace</h2>
        <p>Device, AI and activity panels are separate modules. The optional shared browser and visual surfaces are pending.</p>
      </section>
    </div>
  </>;
}
