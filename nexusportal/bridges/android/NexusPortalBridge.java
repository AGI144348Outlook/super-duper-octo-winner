package org.nexusportal.bridge;

import android.webkit.JavascriptInterface;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Illustrative narrow bridge for a WebView loading only bundled, trusted assets.
 * NOT an overlay, MediaProjection implementation, or AI connection.
 * Host must bind this only to trusted app assets and revoke on navigation.
 */
public final class NexusPortalBridge {
  public interface ConsentGate {
    boolean isSessionActive(String sessionId);
    boolean canAnnotate(String sessionId);
    void onAnnotation(String sessionId, String boundedPayload);
  }
  private final ConsentGate gate;
  public NexusPortalBridge(ConsentGate gate) { this.gate = gate; }

  @JavascriptInterface
  public void submitAnnotation(String message) {
    if (message == null || message.length() > 4096) return;
    try {
      JSONObject envelope = new JSONObject(message);
      if (!"nexusportal.bridge.v0".equals(envelope.optString("version"))) return;
      if (!"annotation".equals(envelope.optString("kind"))) return;
      String id = envelope.optString("sessionId", "");
      if (!id.matches("[a-zA-Z0-9_-]{8,80}")) return;
      if (!gate.isSessionActive(id) || !gate.canAnnotate(id)) return;
      JSONObject payload = envelope.optJSONObject("payload");
      if (payload == null || payload.toString().length() > 2048) return;
      String type = payload.optString("type", "");
      if (!(type.equals("highlight") || type.equals("arrow") || type.equals("circle") || type.equals("text") || type.equals("symbol") || type.equals("clear"))) return;
      gate.onAnnotation(id, payload.toString());
    } catch (JSONException ignored) { /* reject malformed messages */ }
  }
}
