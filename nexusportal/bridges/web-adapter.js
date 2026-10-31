import {VERSION,createSession,setConsent,snapshot,stop,authorizeAnnotation} from './protocol.js';
// Local browser preview broker. This is NOT an AI provider connection.
export function createPreviewBridge(renderer,sessionId='preview_00000001'){
 const s=createSession(sessionId);
 return Object.freeze({version:VERSION,status:()=>snapshot(s),grantAnnotations:enabled=>setConsent(s,'annotating',enabled),pause:()=>{if(!s.stopped)s.paused=true;return snapshot(s)},resume:()=>{if(!s.stopped)s.paused=false;return snapshot(s)},receive:message=>{const p=authorizeAnnotation(s,message);return p?renderer.apply(p):false},stop:()=>{renderer.clear();return stop(s)}});
}
