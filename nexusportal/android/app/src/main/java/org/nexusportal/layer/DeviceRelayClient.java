package org.nexusportal.layer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.json.JSONException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class DeviceRelayClient {
 static final String BASE="https://nexusportal-layer.vercel.app";
 private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
 private static final Handler UI=new Handler(Looper.getMainLooper());
 interface Callback { void done(JSONObject body,String error); }
 interface PairCallback { void done(String code,String error); }
 private DeviceRelayClient(){}
 static String newId(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return hex(b);}
 private static String hex(byte[] b){StringBuilder s=new StringBuilder(b.length*2);for(byte x:b)s.append(String.format(Locale.US,"%02x",x&255));return s.toString();}
 private static String hash(String v)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}
 static String sessionId(Context c){return c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).getString("session_id",null);}
 static boolean paired(Context c){return c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).getBoolean("paired",false)&&SecureDeviceSecretStore.get(c,"token")!=null;}
 static void beginPairing(Context c,PairCallback cb){
  final String code=String.format(Locale.US,"%08d",new SecureRandom().nextInt(100000000));
  final String pairingId=newId(),token=newId();
  try{
   SecureDeviceSecretStore.put(c,"token",token);SecureDeviceSecretStore.put(c,"pairing_id",pairingId);SecureDeviceSecretStore.put(c,"pair_code",code);
  }catch(Exception e){UI.post(()->cb.done(null,"Secure Android storage unavailable"));return;}
  c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).edit().putBoolean("paired",false).putBoolean("voice_active",false).remove("session_id").apply();
  try{
   JSONObject body=new JSONObject().put("pairingId",pairingId).put("codeHash",hash(code)).put("deviceTokenHash",hash(token));
   post("pair-start",null,body,(result,error)->{
    if(error!=null){SecureDeviceSecretStore.remove(c,"token");SecureDeviceSecretStore.remove(c,"pairing_id");SecureDeviceSecretStore.remove(c,"pair_code");cb.done(null,error);}
    else cb.done(code,null);
   });
  }catch(Exception e){cb.done(null,"Could not start pairing");}
 }
 static void checkPairing(Context c,Callback cb){
  String token=SecureDeviceSecretStore.get(c,"token"),id=SecureDeviceSecretStore.get(c,"pairing_id");
  if(token==null||id==null){UI.post(()->cb.done(null,"Start pairing on this phone first"));return;}
  try{post("pair-status",token,new JSONObject().put("pairingId",id),(r,e)->{
   if(e==null&&r.optBoolean("approved",false))c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).edit().putBoolean("paired",true).apply();
   cb.done(r,e);
  });}catch(Exception e){UI.post(()->cb.done(null,"Could not check pairing"));}
 }
 static void setVoiceSession(Context c,boolean active,Callback cb){
  String token=SecureDeviceSecretStore.get(c,"token");
  if(token==null||!paired(c)){UI.post(()->cb.done(null,"Pair this phone first"));return;}
  String sid=active?newId():sessionId(c);
  if(active)c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).edit().putBoolean("voice_active",true).putString("session_id",sid).apply();
  else c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).edit().putBoolean("voice_active",false).apply();
  try{post("heartbeat",token,new JSONObject().put("voiceConsent",active).put("sessionId",active?sid:JSONObject.NULL),(r,e)->{
   if(e!=null&&active)c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).edit().putBoolean("voice_active",false).remove("session_id").apply();
   cb.done(r,e);
  });}catch(Exception e){UI.post(()->cb.done(null,"Could not update voice session"));}
 }
 static void heartbeat(Context c,Callback cb){
  String token=SecureDeviceSecretStore.get(c,"token");
  if(token==null||!paired(c)){UI.post(()->cb.done(null,"Phone not paired"));return;}
  boolean active=c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).getBoolean("voice_active",false);
  String sid=sessionId(c);
  try{post("heartbeat",token,new JSONObject().put("voiceConsent",active).put("sessionId",active&&sid!=null?sid:JSONObject.NULL),cb);}
  catch(Exception e){UI.post(()->cb.done(null,"Heartbeat failed"));}
 }
 static void poll(Context c,Callback cb){
  String token=SecureDeviceSecretStore.get(c,"token");
  if(token==null||!c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).getBoolean("voice_active",false)){UI.post(()->cb.done(null,"Voice session off"));return;}
  try{post("command-poll",token,new JSONObject(),cb);}catch(Exception e){UI.post(()->cb.done(null,"Command poll failed"));}
 }
 static void ack(Context c,String commandId,String sid){
  String token=SecureDeviceSecretStore.get(c,"token");if(token==null)return;
  try{post("command-ack",token,new JSONObject().put("commandId",commandId).put("sessionId",sid),(r,e)->{});}catch(Exception ignored){}
 }
 static void sendTranscript(Context c,String transcript,Callback cb){
  String token=SecureDeviceSecretStore.get(c,"token"),sid=sessionId(c);
  if(token==null||sid==null||!c.getSharedPreferences("nexusportal_device",Context.MODE_PRIVATE).getBoolean("voice_active",false)){UI.post(()->cb.done(null,"Start a paired voice session first"));return;}
  try{post("speech-submit",token,new JSONObject().put("inputId",newId()).put("sessionId",sid).put("transcript",transcript),(r,e)->cb.done(r,e));}
  catch(Exception e){UI.post(()->cb.done(null,"Could not send transcript"));}
 }
 private static void post(String action,String token,JSONObject body,Callback cb){
  EXEC.execute(()->{
   HttpURLConnection conn=null;JSONObject result=null;String error=null;
   try{
    conn=(HttpURLConnection)new URL(BASE+"/api/device/"+action).openConnection();
    conn.setRequestMethod("POST");conn.setInstanceFollowRedirects(false);
    conn.setConnectTimeout(8000);conn.setReadTimeout(8000);conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("Accept","application/json");
    if(token!=null)conn.setRequestProperty("Authorization","Bearer "+token);
    byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
    try(OutputStream out=conn.getOutputStream()){out.write(bytes);}
    int status=conn.getResponseCode();
    InputStream in=status>=200&&status<300?conn.getInputStream():conn.getErrorStream();
    if(in!=null){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;while((n=in.read(b))>0)out.write(b,0,n);result=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));}
    if(status<200||status>=300)error=status==401?"Device authorization rejected":status==404?"Pairing not found or expired":"Relay returned HTTP "+status;
   }catch(Exception e){error="Could not contact the NexusPortal relay";}
   finally{if(conn!=null)conn.disconnect();}
   JSONObject finalResult=result;String finalError=error;UI.post(()->cb.done(finalResult,finalError));
  });
 }
}
