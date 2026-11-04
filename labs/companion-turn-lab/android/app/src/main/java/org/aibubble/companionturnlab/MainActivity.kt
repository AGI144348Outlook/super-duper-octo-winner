package org.aibubble.companionturnlab

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.File
import java.util.UUID

class MainActivity : Activity() {
    private enum class Capture { IDLE, RECORDING, CLOSED, CANCELLED, ERROR }
    private var capture = Capture.IDLE
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var transactionId = UUID.randomUUID().toString()
    private lateinit var status: TextView
    private lateinit var receipt: TextView
    private lateinit var toggle: Button
    private lateinit var cancel: Button
    private val micRequest = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 36, 32, 32)
        }
        val title = TextView(this).apply { text = "Companion Turn Lab — isolated Android app"; textSize = 21f }
        status = TextView(this)
        toggle = Button(this).apply { text = "פ  Start user turn"; setOnClickListener { when(capture) {
            Capture.IDLE -> requestStart()
            Capture.RECORDING -> stop()
            else -> Unit
        } } }
        cancel = Button(this).apply { text = "Cancel"; setOnClickListener { cancel() } }
        val reset = Button(this).apply { text = "New turn"; setOnClickListener {
            if (capture != Capture.RECORDING) {
                recordingFile?.delete()
                recordingFile = null
                transactionId = UUID.randomUUID().toString()
                capture = Capture.IDLE
                render()
            }
        } }
        val enableBubble = Button(this).apply { text = "Enable floating microphone bubble"; setOnClickListener { enableBubble() } }
        val stopBubble = Button(this).apply { text = "Stop floating bubble"; setOnClickListener {
            startService(Intent(this@MainActivity, BubbleService::class.java).setAction(BubbleService.ACTION_STOP))
        } }
        layout.addView(enableBubble)
        layout.addView(stopBubble)
        receipt = TextView(this).apply { textSize = 12f; setTextIsSelectable(true) }
        listOf(title, toggle, cancel, reset, status, receipt).forEach(layout::addView)
        setContentView(layout)
        render()
    }

    private fun enableBubble() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 102)
            return
        }
        startForegroundService(Intent(this, BubbleService::class.java).setAction(BubbleService.ACTION_START))
    }

    private fun requestStart() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
        } else start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 102 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) enableBubble()
        else if (requestCode == micRequest && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && capture == Capture.IDLE) start()
        else render()
    }

    @Suppress("DEPRECATION")
    private fun start() {
        if (capture != Capture.IDLE) return
        try {
            val file = File(cacheDir, "user-turn-${transactionId}.m4a")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordingFile = file
            capture = Capture.RECORDING
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            recordingFile?.delete()
            recordingFile = null
            capture = Capture.ERROR
        }
        render()
    }

    private fun stop() {
        if (capture != Capture.RECORDING) return
        try {
            recorder?.stop()
            capture = Capture.CLOSED
        } catch (e: Exception) {
            recordingFile?.delete()
            recordingFile = null
            capture = Capture.ERROR
        } finally {
            recorder?.release()
            recorder = null
        }
        render()
    }

    private fun cancel() {
        if (capture != Capture.RECORDING) return
        try { recorder?.stop() } catch (_: Exception) { }
        recorder?.release()
        recorder = null
        recordingFile?.delete()
        recordingFile = null
        capture = Capture.CANCELLED
        render()
    }

    override fun onStop() {
        super.onStop()
        if (capture == Capture.RECORDING) cancel() // never silently dispatch after backgrounding
    }

    private fun render() {
        toggle.isEnabled = capture == Capture.IDLE || capture == Capture.RECORDING
        toggle.text = if (capture == Capture.RECORDING) "■  End user turn" else "פ  Start user turn"
        cancel.isEnabled = capture == Capture.RECORDING
        status.text = "Capture: ${capture.name}. Silence never submits a turn. No companion adapter connected."
        val requests = JSONObject()
        requests.put("R1_transcription_to_chat", JSONObject().put("status", if(capture == Capture.CLOSED) "UNSUPPORTED" else "PENDING"))
        requests.put("R2_companion_response", JSONObject().put("status", "PENDING"))
        requests.put("R3_companion_spoken_response_record", JSONObject().put("status", "PENDING"))
        val root = JSONObject()
            .put("contract", "CONTRACT-{•}")
            .put("transaction_id", transactionId)
            .put("capture_state", capture.name)
            .put("requests", requests)
            .put("relay_delivery_acknowledged", false)
            .put("overall_status", when(capture) {
                Capture.CANCELLED -> "CANCELLED"
                Capture.CLOSED -> "UNSUPPORTED"
                Capture.ERROR -> "FAILED"
                else -> "PENDING"
            })
        receipt.text = root.toString(2)
    }
}
