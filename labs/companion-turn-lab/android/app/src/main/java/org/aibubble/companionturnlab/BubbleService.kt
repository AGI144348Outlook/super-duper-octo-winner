package org.aibubble.companionturnlab

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.MediaRecorder
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.TextView
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * User-operated microphone bubble. No silence detection, no automatic dispatch.
 * All audio is local. No companion transport is implemented.
 */
class BubbleService : Service() {
    companion object {
        const val ACTION_START = "org.aibubble.companionturnlab.START"
        const val ACTION_STOP = "org.aibubble.companionturnlab.STOP"
        const val ACTION_CANCEL = "org.aibubble.companionturnlab.CANCEL"
        private const val CHANNEL = "lab-microphone"
        private const val NOTIFICATION_ID = 42
    }
    private var manager: WindowManager? = null
    private var bubble: TextView? = null
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var recording = false
    private var transactionId = UUID.randomUUID().toString()

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        val channels = getSystemService(NotificationManager::class.java)
        channels.createNotificationChannel(NotificationChannel(CHANNEL, "Companion Lab microphone", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("Companion Turn Lab")
            .setContentText("User-controlled microphone bubble")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(NOTIFICATION_ID, notification)
        showBubble()
    }

    private fun showBubble() {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        manager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            text = "פ"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xffffffff.toInt())
            setBackgroundColor(0xff26324a.toInt())
            contentDescription = "Start recording; tap again to stop"
            setOnClickListener { if (recording) stopRecording() else startRecording() }
        }
        val params = WindowManager.LayoutParams(
            160, 160, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 240 }
        manager?.addView(view, params)
        bubble = view
    }

    @Suppress("DEPRECATION")
    private fun startRecording() {
        if (recording || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        try {
            transactionId = UUID.randomUUID().toString()
            val output = File(cacheDir, "bubble-${transactionId}.m4a")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(output.absolutePath)
            r.prepare()
            r.start()
            file = output
            recorder = r
            recording = true
            bubble?.text = "■"
            bubble?.contentDescription = "Stop recording and close user turn"
        } catch (_: Exception) {
            recorder?.release()
            recorder = null
            file?.delete()
            file = null
            recording = false
            bubble?.text = "!"
        }
    }

    private fun stopRecording() {
        if (!recording) return
        var closed = false
        try { recorder?.stop(); closed = true }
        catch (_: Exception) { file?.delete(); file = null }
        finally { recorder?.release(); recorder = null; recording = false }
        bubble?.text = "פ"
        bubble?.contentDescription = "Start recording"
        writeReceipt(if (closed) "CAPTURE_CLOSED" else "CAPTURE_FAILED")
    }

    private fun cancelRecording() {
        if (recording) {
            try { recorder?.stop() } catch (_: Exception) {}
            recorder?.release()
            recorder = null
            recording = false
        }
        file?.delete()
        file = null
        bubble?.text = "פ"
        writeReceipt("CANCELLED")
    }

    private fun writeReceipt(state: String) {
        val root = JSONObject()
            .put("contract", "CONTRACT-{•}")
            .put("transaction_id", transactionId)
            .put("capture_state", state)
            .put("requests", JSONObject()
                .put("R1_transcription_to_chat", JSONObject().put("status", if (state == "CAPTURE_CLOSED") "UNSUPPORTED" else "PENDING"))
                .put("R2_companion_response", JSONObject().put("status", "PENDING"))
                .put("R3_spoken_response_record", JSONObject().put("status", "PENDING")))
            .put("relay_delivery_acknowledged", false)
            .put("overall_status", when (state) { "CANCELLED" -> "CANCELLED"; "CAPTURE_CLOSED" -> "UNSUPPORTED"; else -> "FAILED" })
        File(filesDir, "last-bubble-receipt.json").writeText(root.toString(2))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { cancelRecording(); stopSelf() }
            ACTION_CANCEL -> cancelRecording()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (recording) cancelRecording()
        bubble?.let { try { manager?.removeView(it) } catch (_: Exception) {} }
        bubble = null
        super.onDestroy()
    }
}
