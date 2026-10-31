package org.nexusportal.layer;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;
import org.json.JSONObject;

public class OverlayService extends Service {
 private static final String CHANNEL = "nexusportal_overlay";
 private WindowManager wm;
 private View bubble, layer, dismissTarget;
 private WindowManager.LayoutParams bubbleParams;
 private boolean shown = false;
 private boolean sessionActive = false;
 private boolean annotationAuthorized = false;
 private TextToSpeech speaker;
 private boolean speakerReady=false;
 private boolean relayPollInFlight=false;
 private long lastRelayHeartbeat=0;
 private void publishSessionState() { getSharedPreferences("nexusportal_session", MODE_PRIVATE).edit().putBoolean("active", sessionActive).putBoolean("annotations", sessionActive && annotationAuthorized).apply(); }
 private final Handler handler = new Handler(Looper.getMainLooper());

 private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
 private int screenWidth() { return getResources().getDisplayMetrics().widthPixels; }
 private int screenHeight() { return getResources().getDisplayMetrics().heightPixels; }
 private int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }

 @Override public void onCreate() {
  super.onCreate();
  if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
  wm = (WindowManager) getSystemService(WINDOW_SERVICE);
  createNotification();
  createWindows();
  publishSessionState();
  speaker=new TextToSpeech(this,result->{speakerReady=result==TextToSpeech.SUCCESS;if(speakerReady)speaker.setLanguage(Locale.getDefault());});
  startRelayTicker();
 }

 private final Runnable relayTick=new Runnable(){@Override public void run(){
  if(DeviceRelayClient.paired(OverlayService.this)){
   long now=System.currentTimeMillis();
   if(now-lastRelayHeartbeat>10000){lastRelayHeartbeat=now;DeviceRelayClient.heartbeat(OverlayService.this,(r,e)->{});}
   boolean active=getSharedPreferences("nexusportal_device",MODE_PRIVATE).getBoolean("voice_active",false);
   if(active&&speakerReady&&!relayPollInFlight){
    relayPollInFlight=true;
    DeviceRelayClient.poll(OverlayService.this,(r,e)->{
     relayPollInFlight=false;
     if(e==null&&r!=null){JSONObject command=r.optJSONObject("command");if(command!=null)playRelayCommand(command);}
    });
   }
  }
  if(handler!=null)handler.postDelayed(this,2500);
 }};
 private void startRelayTicker(){handler.removeCallbacks(relayTick);handler.post(relayTick);}
 private void playRelayCommand(JSONObject command){
  String commandId=command.optString("command_id","");
  String sessionId=command.optString("session_id","");
  String current=DeviceRelayClient.sessionId(this);
  JSONObject payload=command.optJSONObject("payload");
  String text=payload==null?"":payload.optString("text","");
  if(!"speak_text".equals(command.optString("kind"))||text.isEmpty()||!sessionId.equals(current)||!speakerReady){
   return;
  }
  speaker.setOnUtteranceProgressListener(new UtteranceProgressListener(){
   @Override public void onStart(String id){}
   @Override public void onDone(String id){DeviceRelayClient.ack(OverlayService.this,commandId,sessionId);}
   @Override public void onError(String id){DeviceRelayClient.ack(OverlayService.this,commandId,sessionId);}
  });
  speaker.speak(text,TextToSpeech.QUEUE_ADD,null,commandId);
 }
 private void createNotification() {
  NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
  nm.createNotificationChannel(new NotificationChannel(CHANNEL, "NexusPortal overlay", NotificationManager.IMPORTANCE_LOW));
  Intent stop = new Intent(this, OverlayService.class);
  stop.setAction("STOP");
  PendingIntent stopPi = PendingIntent.getService(this, 0, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  Intent config = new Intent(this, MainActivity.class);
  config.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent configPi = PendingIntent.getActivity(this, 1, config, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  Notification n = new Notification.Builder(this, CHANNEL)
   .setContentTitle("NexusPortal overlay active")
   .setContentText("Tap to configure · Stop is always available")
   .setSmallIcon(android.R.drawable.ic_menu_view)
   .setContentIntent(configPi)
   .setOngoing(true)
   .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi).build())
   .build();
  startForeground(17, n);
 }

 private void openConfiguration() {
  Intent config = new Intent(this, MainActivity.class);
  config.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
  config.putExtra("companion_panel",true);
  startActivity(config);
 }

 private void onBubbleTap() {
  android.content.SharedPreferences prefs=getSharedPreferences("nexusportal_companion",MODE_PRIVATE);
  if(prefs.getBoolean("app_open",false)) {
   prefs.edit().putBoolean("app_open",false).apply();
   Intent home=new Intent(Intent.ACTION_MAIN);
   home.addCategory(Intent.CATEGORY_HOME);
   home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   try { startActivity(home); } catch(Exception e) { openConfiguration(); }
  } else openConfiguration();
 }
 private void setDismissVisible(boolean visible) {
  if (dismissTarget != null) dismissTarget.setVisibility(visible ? View.VISIBLE : View.GONE);
 }

 private void createWindows() {
  int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
  WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
   -1, -1, type,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
   PixelFormat.TRANSLUCENT);
  layer = new View(this) {
   final Paint p = new Paint(3);
   @Override protected void onDraw(Canvas c) {
    super.onDraw(c);
    if (!shown) return;
    p.setColor(Color.argb(190, 109, 227, 212));
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(dp(5));
    float w = getWidth(), h = getHeight();
    c.drawCircle(w * .5f, h * .45f, Math.min(w, h) * .19f, p);
    c.drawLine(w * .2f, h * .67f, w * .78f, h * .54f, p);
   }
  };
  wm.addView(layer, lp);

  TextView target = new TextView(this);
  target.setText("✕  Release to stop");
  target.setTextSize(16);
  target.setTextColor(Color.WHITE);
  target.setGravity(Gravity.CENTER);
  target.setBackgroundColor(Color.argb(225, 150, 42, 56));
  WindowManager.LayoutParams tp = new WindowManager.LayoutParams(
   dp(190), dp(56), type,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
   PixelFormat.TRANSLUCENT);
  tp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
  tp.y = dp(56);
  wm.addView(target, tp);
  dismissTarget = target;
  setDismissVisible(false);

  TextView b = new TextView(this);
  b.setText("%");
  b.setTextSize(26);
  b.setTextColor(Color.rgb(17, 34, 53));
  b.setGravity(Gravity.CENTER);
  b.setBackgroundColor(Color.rgb(109, 227, 212));
  bubbleParams = new WindowManager.LayoutParams(
   dp(56), dp(56), type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
  bubbleParams.gravity = Gravity.TOP | Gravity.LEFT;
  bubbleParams.x = dp(24);
  bubbleParams.y = dp(190);

  final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
  b.setOnTouchListener(new View.OnTouchListener() {
   float downRawX, downRawY;
   int startX, startY;
   boolean dragging, longPressed;
   Runnable longPress;

   @Override public boolean onTouch(View v, android.view.MotionEvent event) {
    switch (event.getActionMasked()) {
     case MotionEvent.ACTION_DOWN:
      downRawX = event.getRawX();
      downRawY = event.getRawY();
      startX = bubbleParams.x;
      startY = bubbleParams.y;
      dragging = false;
      longPressed = false;
      longPress = () -> { if (!dragging) { longPressed = true; stopSelf(); } };
      handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
      return true;
     case MotionEvent.ACTION_MOVE:
      int dx = Math.round(event.getRawX() - downRawX);
      int dy = Math.round(event.getRawY() - downRawY);
      if (!dragging && (Math.abs(dx) > slop || Math.abs(dy) > slop)) {
       dragging = true;
       handler.removeCallbacks(longPress);
       setDismissVisible(true);
      }
      if (dragging && wm != null) {
       bubbleParams.x = clamp(startX + dx, 0, Math.max(0, screenWidth() - bubbleParams.width));
       bubbleParams.y = clamp(startY + dy, 0, Math.max(0, screenHeight() - bubbleParams.height));
       wm.updateViewLayout(bubble, bubbleParams);
      }
      return true;
     case MotionEvent.ACTION_UP:
      handler.removeCallbacks(longPress);
      if (longPressed) return true;
      if (dragging) {
       setDismissVisible(false);
       // The drop zone spans the bottom center, not the entire lower screen.
       int centerX = bubbleParams.x + bubbleParams.width / 2;
       int centerY = bubbleParams.y + bubbleParams.height / 2;
       if (centerY >= screenHeight() - dp(140) &&
           Math.abs(centerX - screenWidth() / 2) <= dp(120)) {
        stopSelf();
       } else {
        bubbleParams.x = centerX < screenWidth() / 2 ? dp(8) : Math.max(0, screenWidth() - bubbleParams.width - dp(8));
        wm.updateViewLayout(bubble, bubbleParams);
       }
      } else {
       onBubbleTap();
      }
      return true;
     case MotionEvent.ACTION_CANCEL:
      handler.removeCallbacks(longPress);
      setDismissVisible(false);
      return true;
    }
    return true;
   }
  });
  wm.addView(b, bubbleParams);
  bubble = b;
 }

 @Override public int onStartCommand(Intent intent, int flags, int startId) {
  if (intent != null) {
   String action = intent.getAction();
   if ("STOP".equals(action)) { revokeAnnotations(); annotationAuthorized = false; sessionActive = false; publishSessionState(); stopSelf(); return START_NOT_STICKY; }
   if ("START".equals(action)) { sessionActive = true; annotationAuthorized = false; revokeAnnotations(); }
   if ("VOICE_START".equals(action)) { startRelayTicker(); }
   if ("VOICE_STOP".equals(action)) {
    if(speaker!=null)speaker.stop();
    DeviceRelayClient.setVoiceSession(this,false,(r,e)->{});
   }
   if ("ENSURE_STARTED".equals(action) && !sessionActive) { sessionActive = true; annotationAuthorized = false; revokeAnnotations(); }
   if ("AUTHORIZE_ANNOTATIONS".equals(action) && sessionActive) annotationAuthorized = true;
   if ("REVOKE_ANNOTATIONS".equals(action)) { annotationAuthorized = false; revokeAnnotations(); }
   publishSessionState();
   if ("START".equals(action) || "ENSURE_STARTED".equals(action)) startRelayTicker();
   if (sessionActive && annotationAuthorized && "SHOW_ANNOTATIONS".equals(action)) { shown = true; if (layer != null) layer.invalidate(); }
   if (sessionActive && annotationAuthorized && "TOGGLE_ANNOTATIONS".equals(action)) { shown = !shown; if (layer != null) layer.invalidate(); }
   if ("HIDE_ANNOTATIONS".equals(action)) { shown = false; if (layer != null) layer.invalidate(); }
  }
  return START_NOT_STICKY;
 }

 private void revokeAnnotations() { shown = false; if (layer != null) layer.invalidate(); }

 @Override public void onDestroy() {
  revokeAnnotations();
  annotationAuthorized = false;
  sessionActive = false;
  publishSessionState();
  getSharedPreferences("nexusportal_companion",MODE_PRIVATE).edit().putBoolean("app_open",false).apply();
  handler.removeCallbacksAndMessages(null);
  if(getSharedPreferences("nexusportal_device",MODE_PRIVATE).getBoolean("voice_active",false))DeviceRelayClient.setVoiceSession(this,false,(r,e)->{});
  if(speaker!=null){speaker.stop();speaker.shutdown();speaker=null;}
  if (wm != null) {
   if (bubble != null) wm.removeView(bubble);
   if (dismissTarget != null) wm.removeView(dismissTarget);
   if (layer != null) wm.removeView(layer);
  }
  super.onDestroy();
 }

 @Override public IBinder onBind(Intent intent) { return null; }
}
