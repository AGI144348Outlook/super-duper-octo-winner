package org.nexusportal.layer;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
 private TextView status;
 private TextView networkStatus;
 private TextView voiceStatus;
 private EditText endpointInput;
 private EditText keyInput;
 private String lastTranscript="";
 private TextToSpeech speech;
 private boolean speechReady=false;
 private static final int SPEECH_REQUEST=401;
 private LinearLayout root;
 private boolean annotationPermission=false;
 private TextView consentStatus;
 private Switch annotationConsent;
 private boolean syncing=false;
 private final int bg=Color.rgb(16,24,39), accent=Color.rgb(109,227,212);
 private TextView heading(String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(0,18,0,10);return t;}
 private Button action(String label,View.OnClickListener click){Button b=new Button(this);b.setText(label);b.setOnClickListener(click);root.addView(b);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);speech=new TextToSpeech(this,result->{speechReady=result==TextToSpeech.SUCCESS;if(speechReady)speech.setLanguage(Locale.getDefault());});showConfiguration();}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);showConfiguration();}
 @Override protected void onResume(){super.onResume();if(status!=null)status.setText("Screen observation OFF · Touch control OFF");refreshRelayStatus();syncSessionState();}
 private void refreshRelayStatus(){
  if(networkStatus==null)return;
  if(!DeviceRelayClient.paired(this)){
   String pending=SecureDeviceSecretStore.get(this,"pair_code");
   networkStatus.setText(pending==null?"Phone not paired · Tap Pair this phone to connect":"Phone not paired · Approve code "+pending+" in ChatGPT, then check pairing");
   return;
  }
  DeviceRelayClient.heartbeat(this,(result,error)->{
   if(networkStatus!=null)networkStatus.setText(error==null?"Relay connected · Phone paired · "+(getSharedPreferences("nexusportal_device",MODE_PRIVATE).getBoolean("voice_active",false)?"Voice session active":"Voice off"):"Phone paired · Relay disconnected");
  });
 }
 private void syncSessionState(){if(annotationConsent==null)return;boolean active=getSharedPreferences("nexusportal_session",MODE_PRIVATE).getBoolean("active",false);boolean allowed=active && getSharedPreferences("nexusportal_session",MODE_PRIVATE).getBoolean("annotations",false);syncing=true;annotationConsent.setChecked(allowed);annotationPermission=allowed;syncing=false;if(consentStatus!=null)consentStatus.setText((allowed?"Annotations: granted":"Annotations: denied")+(active?" · Local session active":" · No active session")+" · MCP disconnected");}
 private void showConfiguration(){
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(36,44,36,48);root.setBackgroundColor(bg);scroll.addView(root);
  root.addView(heading("NexusPortal  %",26));root.addView(heading("AI configuration",20));
  status=heading("Personal device prototype · MCP not connected · screen observation OFF",14);status.setTextColor(Color.LTGRAY);root.addView(status);
  root.addView(heading("AI companion launcher",18));
  TextView launchNote=heading("ChatGPT Voice stays in the ChatGPT app. Enable Background Conversations in ChatGPT Settings > Voice, open ChatGPT, and start Voice. Tap the floating % to minimize to Home; tap % again to return to that chat. NexusPortal does not capture or transfer the conversation.",14);launchNote.setTextColor(Color.LTGRAY);root.addView(launchNote);
  action("ChatGPT · Open app (not connected)",v->launchCompanion("ChatGPT","com.openai.chatgpt"));
  action("Claude · Open app (not connected)",v->launchCompanion("Claude","com.anthropic.claude"));
  action("Gemini · Open app (not connected)",v->launchCompanion("Gemini","com.google.android.apps.bard"));
  root.addView(heading("My AI connections",18));
  TextView note=heading("ChatGPT MCP relay connection is separate from pairing. Phone and voice status appear below.",14);note.setTextColor(Color.LTGRAY);root.addView(note);
  root.addView(heading("Phone ↔ ChatGPT connection",18));
  TextView mcpNote=heading("Pair this phone with a one-time code. No long-lived token is typed or shown. Pairing is separate from voice consent; screen observation and touch control stay OFF.",14);mcpNote.setTextColor(Color.LTGRAY);root.addView(mcpNote);
  networkStatus=heading("Relay status: disconnected · Phone: not paired · Voice: off",14);networkStatus.setTextColor(Color.LTGRAY);root.addView(networkStatus);
  action("Pair this phone",v->{
   networkStatus.setText("Requesting a one-time pairing code...");
   DeviceRelayClient.beginPairing(this,(code,error)->{
    if(networkStatus==null)return;
    if(error!=null){networkStatus.setText("Pairing could not start: "+error);return;}
    networkStatus.setText("Pair code: "+code+" · In this ChatGPT NexusPortal chat, ask me to approve this code. Expires in 10 minutes.");
   });
  });
  action("Check pairing approval",v->{
   networkStatus.setText("Checking pairing...");
   DeviceRelayClient.checkPairing(this,(result,error)->{
    if(networkStatus==null)return;
    if(error!=null){networkStatus.setText("Phone not paired · "+error);return;}
    if(result!=null&&result.optBoolean("approved",false)){SecureDeviceSecretStore.remove(this,"pair_code");networkStatus.setText("Relay connected · Phone paired · Voice off");}
    else networkStatus.setText("Pair code is waiting for approval in ChatGPT.");
   });
  });
  action("Start voice session",v->{
   if(!DeviceRelayClient.paired(this)){networkStatus.setText("Pair this phone before starting a voice session.");return;}
   networkStatus.setText("Starting voice session...");
   DeviceRelayClient.setVoiceSession(this,true,(result,error)->{
    if(error!=null){networkStatus.setText("Voice session did not start: "+error);return;}
    sendOverlay("VOICE_START");
    networkStatus.setText("Relay connected · Phone paired · Voice session active");
   });
  });
  action("Stop voice session",v->{
   DeviceRelayClient.setVoiceSession(this,false,(result,error)->{
    sendOverlay("VOICE_STOP");
    networkStatus.setText(error==null?"Relay connected · Phone paired · Voice off":"Voice stop requested · "+error);
   });
  });
  action("Revoke and forget this phone",v->{
   DeviceRelayClient.revoke(this,(result,error)->{
    sendOverlay("VOICE_STOP");
    SecureDeviceSecretStore.clear(this);
    getSharedPreferences("nexusportal_device",MODE_PRIVATE).edit().clear().apply();
    networkStatus.setText(error==null?"Phone revoked · Not connected":"Local credentials cleared · Server revoke could not be confirmed");
   });
  });
  root.addView(heading("Voice (tap to listen; bubble speaks replies)",18));
  TextView voiceNote=heading("Tap to open Android speech recognition. Audio goes only to the Android-selected recognizer; NexusPortal sends no raw audio. After recognition, tap Send transcript to ChatGPT. ChatGPT can then speak its reply through the bubble during your active voice session.",14);voiceNote.setTextColor(Color.LTGRAY);root.addView(voiceNote);
  voiceStatus=heading("Microphone inactive. Speaker idle.",14);voiceStatus.setTextColor(Color.LTGRAY);root.addView(voiceStatus);
  action("Tap to speak (Android speech dialog)",v->{
   Intent listen=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   listen.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   listen.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault().toLanguageTag());
   listen.putExtra(RecognizerIntent.EXTRA_PROMPT,"NexusPortal local voice preview");
   try { startActivityForResult(listen,SPEECH_REQUEST);voiceStatus.setText("Speech dialog opened; microphone permission handled by the provider."); }
   catch(Exception e){voiceStatus.setText("No compatible speech recognizer available on this phone.");}
  });
  action("Send last transcript to ChatGPT",v->{
   if(lastTranscript.isEmpty()){voiceStatus.setText("No transcript to send.");return;}
   voiceStatus.setText("Sending transcript with your active session consent...");
   DeviceRelayClient.sendTranscript(this,lastTranscript,(result,error)->{
    if(voiceStatus!=null)voiceStatus.setText(error==null?"Transcript sent to the active ChatGPT relay session. Ask ChatGPT to retrieve it.":"Transcript not sent: "+error);
   });
  });
  action("Read last transcript aloud",v->{
   if(lastTranscript.isEmpty()){voiceStatus.setText("No local transcript yet.");return;}
   if(!speechReady){voiceStatus.setText("Text-to-speech engine not available.");return;}
   speech.speak(lastTranscript,TextToSpeech.QUEUE_FLUSH,null,"nexusportal-preview");
   voiceStatus.setText("Playing local transcript (not an AI response).");
  });
  action("Stop voice and clear transcript",v->{
   if(speech!=null)speech.stop();lastTranscript="";
   if(voiceStatus!=null)voiceStatus.setText("Microphone inactive; local transcript cleared.");
  });
  root.addView(heading("Session controls",18));
  annotationConsent=new Switch(this);
  annotationConsent.setText("Allow AI annotations in this session");
  annotationConsent.setTextColor(Color.WHITE);
  annotationConsent.setChecked(false);
  root.addView(annotationConsent);
  consentStatus=heading("Annotations: denied · No remote connection",14);
  consentStatus.setTextColor(Color.LTGRAY);
  root.addView(consentStatus);
  annotationConsent.setOnCheckedChangeListener((button,checked)->{if(syncing)return;annotationPermission=checked;consentStatus.setText(checked?"Annotations: permitted locally · No remote connection":"Annotations: denied · No remote connection");if(checked){startOverlay();if(Settings.canDrawOverlays(this))sendOverlay("AUTHORIZE_ANNOTATIONS");}else sendOverlay("REVOKE_ANNOTATIONS");});
  action("Preview authorized annotation (local only)",v->{if(!annotationPermission){status.setText("Enable annotation permission before previewing.");return;}sendOverlay("SHOW_ANNOTATIONS");status.setText("Local preview only. No ChatGPT or MCP connection.");});
  action("Start floating %",v->{annotationConsent.setChecked(false);startOverlay();});
  action("Show / hide sample annotation",v->{if(!annotationPermission){status.setText("Annotation permission is OFF.");return;}sendOverlay("TOGGLE_ANNOTATIONS");});
  action("Pause annotations",v->sendOverlay("HIDE_ANNOTATIONS"));
  action("Stop session and remove overlay",v->{annotationConsent.setChecked(false);sendOverlay("STOP");status.setText("Session stopped; annotation permission revoked.");});
  root.addView(heading("Permissions",18));
  TextView permissions=heading("Overlay: user-granted Android permission.\nScreen observation: OFF (not implemented).\nAI commands: OFF (not connected).\nAnnotation permission: local session only.\nTouch control: not available.",14);permissions.setTextColor(Color.LTGRAY);root.addView(permissions);
  setContentView(scroll);
  if(getIntent().getBooleanExtra("companion_panel",false))showCompanionPanel();
  syncSessionState();
 }
 private void launchCompanion(String name,String pkg){
  Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
  if(launch==null){status.setText(name+" app not found or launch unavailable. No connection made.");return;}
  if(!Settings.canDrawOverlays(this)){startOverlay();return;}
  sendOverlay("ENSURE_STARTED");
  getSharedPreferences("nexusportal_companion",MODE_PRIVATE).edit().putString("name",name).putString("package",pkg).putBoolean("app_open",true).apply();
  launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try{startActivity(launch);}catch(Exception e){getSharedPreferences("nexusportal_companion",MODE_PRIVATE).edit().putBoolean("app_open",false).apply();status.setText("Could not open "+name+".");}
 }
 private void showCompanionPanel(){
  String name=getSharedPreferences("nexusportal_companion",MODE_PRIVATE).getString("name","None");
  root.addView(heading("Companion: "+name+" · MCP disconnected",18));
  action("Return to selected AI app",v->{String pkg=getSharedPreferences("nexusportal_companion",MODE_PRIVATE).getString("package","");if(!pkg.isEmpty())launchCompanion(name,pkg);});
  action("Clear companion selection",v->{getSharedPreferences("nexusportal_companion",MODE_PRIVATE).edit().clear().apply();status.setText("Companion selection cleared.");});
 }
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
  super.onActivityResult(requestCode,resultCode,data);
  if(requestCode==SPEECH_REQUEST){
   if(resultCode==RESULT_OK && data!=null){
    ArrayList<String> text=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
    lastTranscript=text!=null && !text.isEmpty()?text.get(0):"";
    if(voiceStatus!=null)voiceStatus.setText(lastTranscript.isEmpty()?"No speech recognized.":"Recognized by Android. Tap Send transcript to ChatGPT to share this text.");
   } else if(voiceStatus!=null) voiceStatus.setText("Speech canceled. Microphone inactive.");
  }
 }
 @Override protected void onDestroy(){if(speech!=null){speech.stop();speech.shutdown();speech=null;}super.onDestroy();}
 private void sendOverlay(String command){Intent i=new Intent(this,OverlayService.class);i.setAction(command);if("STOP".equals(command)){stopService(i);return;}if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
 private void startOverlay(){
  if(!Settings.canDrawOverlays(this)){status.setText("Grant Draw over other apps, return, then tap Start floating %.");Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);return;}
  sendOverlay("START");status.setText("Overlay started. Tap floating % to reopen this configuration screen.");
 }
}
