package org.nexusportal.layer;

import org.json.JSONObject;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

/** One-shot, opt-in MCP JSON-RPC reachability check. Not a device pairing or command channel. */
final class PortalClient {
 interface Callback { void complete(boolean authenticated, String message); }
 private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
 static void check(String endpoint, String token, Callback callback) {
  final Handler ui = new Handler(Looper.getMainLooper());
  EXEC.execute(() -> {
   String message = "Connection failed";
   boolean valid = false;
   HttpURLConnection conn = null;
   try {
    URL url = new URL(endpoint.trim());
    if (!"https".equalsIgnoreCase(url.getProtocol()) || url.getUserInfo()!=null ||
        url.getQuery()!=null || url.getRef()!=null || !url.getPath().matches("/api/mcp/?")) {
      throw new IllegalArgumentException("Use an HTTPS endpoint ending /api/mcp (no key in URL)");
    }
    if (token.trim().length() < 32) throw new IllegalArgumentException("Enter a separate, revocable bearer token");
    conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Authorization","Bearer "+token.trim());
    conn.setRequestProperty("Content-Type","application/json");
    conn.setRequestProperty("Accept","application/json");
    conn.setConnectTimeout(8000);
    conn.setReadTimeout(8000);
    conn.setDoOutput(true);
    byte[] bytes = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}".getBytes(StandardCharsets.UTF_8);
    try(OutputStream out=conn.getOutputStream()) { out.write(bytes); }
    int status=conn.getResponseCode();
    if(status==200) {
     try(InputStream in=conn.getInputStream()) {
      byte[] reply=new byte[4096]; int n=in.read(reply);
      if(n>0) {
       JSONObject response=new JSONObject(new String(reply,0,n,StandardCharsets.UTF_8));
       valid=response.optInt("id",-1)==1 && response.has("result") && !response.has("error");
      }
     }
     message=valid?"MCP authenticated (ping only; Android session NOT paired)":"Unexpected MCP response";
    } else message=status==401?"Unauthorized: check Android-specific token":"Server responded HTTP "+status;
   } catch(Exception e) { message=e instanceof IllegalArgumentException ? e.getMessage() : "Could not contact the HTTPS MCP endpoint"; }
   finally { if(conn!=null) conn.disconnect(); }
   final boolean success=valid;final String result=message;
   ui.post(()->callback.complete(success,result));
  });
 }
}
