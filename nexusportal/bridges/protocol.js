// NexusPortal v0 message contract. No remote actions or screen capture.
export const VERSION='nexusportal.bridge.v0';
export function createSession(id){if(typeof id!=='string'||!/^[a-zA-Z0-9_-]{8,80}$/.test(id))throw Error('invalid session id');return {id,observing:false,annotating:false,paused:false,stopped:false}}
export function setConsent(s,kind,enabled){if(s.stopped)throw Error('session stopped');if(!['observing','annotating'].includes(kind)||typeof enabled!=='boolean')throw Error('invalid consent');s[kind]=enabled;return snapshot(s)}
export function snapshot(s){return Object.freeze({id:s.id,observing:s.observing,annotating:s.annotating,paused:s.paused,stopped:s.stopped})}
export function stop(s){s.observing=false;s.annotating=false;s.paused=true;s.stopped=true;return snapshot(s)}
export function authorizeAnnotation(s,envelope){if(!envelope||typeof envelope!=='object'||envelope.version!==VERSION||envelope.sessionId!==s.id||envelope.kind!=='annotation'||!s.annotating||s.paused||s.stopped)return null;const p=envelope.payload;if(!p||typeof p!=='object'||Array.isArray(p))return null;return p}
