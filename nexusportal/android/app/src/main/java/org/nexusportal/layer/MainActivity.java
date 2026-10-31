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

public class MainActivity extends Activity {
 private TextView status;
 private LinearLayout root;
 private final int bg=Color.rgb(16,24,39), accent=Color.rgb(109,227,212);
 private TextView heading(String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(0,18,0,10);return t;}
 private Button action(String label,View.OnClickListener click){Button b=new Button(this);b.setText(label);b.setOnClickListener(click);root.addView(b);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);showConfiguration();}
 @Override protected void onResume(){super.onResume();if(status!=null)status.setText("Personal device prototype · MCP not connected · screen observation OFF");}
 private void showConfiguration(){
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(36,44,36,48);root.setBackgroundColor(bg);scroll.addView(root);
  root.addView(heading("NexusPortal  %",26));root.addView(heading("AI configuration",20));
  status=heading("Personal device prototype · MCP not connected · screen observation OFF",14);status.setTextColor(Color.LTGRAY);root.addView(status);
  root.addView(heading("My AI connections",18));
  TextView note=heading("MCP connection slots will appear here once verified. No AI platform is connected yet.",14);note.setTextColor(Color.LTGRAY);root.addView(note);
  root.addView(heading("Session controls",18));
  action("Start floating %",v->startOverlay());
  action("Show / hide sample annotation",v->sendOverlay("TOGGLE_ANNOTATIONS"));
  action("Pause annotations",v->sendOverlay("HIDE_ANNOTATIONS"));
  action("Stop session and remove overlay",v->{sendOverlay("STOP");status.setText("Session stopped.");});
  root.addView(heading("Permissions",18));
  TextView permissions=heading("Overlay: user-granted Android permission.\nScreen observation: OFF (not implemented).\nAI commands: OFF (not connected).\nTouch control: not available.",14);permissions.setTextColor(Color.LTGRAY);root.addView(permissions);
  setContentView(scroll);
 }
 private void sendOverlay(String command){Intent i=new Intent(this,OverlayService.class);i.setAction(command);if("STOP".equals(command)){stopService(i);return;}if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
 private void startOverlay(){
  if(!Settings.canDrawOverlays(this)){status.setText("Grant Draw over other apps, return, then tap Start floating %.");Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);return;}
  sendOverlay("START");status.setText("Overlay started. Tap floating % to reopen this configuration screen.");
 }
}
