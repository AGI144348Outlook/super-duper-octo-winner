package com.aibubble

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.text.InputType
import android.util.Base64
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject
import java.net.URL
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/**
 * Optional portal pairing for the existing app. The older relay and the
 * Sight/Sound controls remain separate from this device status credential.
 */
object PortalPairing {
    private const val origin = "https://aibubble-connect.vercel.app"
    private const val keyAlias = "aibubble-connect-device"
    private const val preferences = "aibubble-connect"

    fun show(activity: Activity) {
        val field = EditText(activity).apply {
            hint = "1234-ABCD-5678"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        AlertDialog.Builder(activity)
            .setTitle("Pair with NexusBoard")
            .setMessage("Generate a code in your own NexusBoard account. Approve this device there after submitting the code.")
            .setView(field)
            .setPositiveButton("Request pairing") { _, _ ->
                val code = field.text.toString().trim()
                if (readCredential(activity) != null) {
                    notice(activity, "Check or forget the current pairing before creating a new one.")
                } else if (!Regex("^[A-Fa-f0-9]{4}-?[A-Fa-f0-9]{4}-?[A-Fa-f0-9]{4}$").matches(code)) {
                    notice(activity, "Enter a valid 12-character code.")
                } else {
                    Thread {
                        try {
                            val token = ByteArray(32).apply { SecureRandom().nextBytes(this) }
                                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                            saveCredential(activity, token)
                            val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
                            val payload = JSONObject()
                                .put("code", code)
                                .put("credential", token)
                                .put("label", (Build.MANUFACTURER + " " + Build.MODEL).trim().take(64))
                                .put("version", info.versionName ?: "unknown")
                            val (status, _) = request("claim", payload)
                            if (status in 200..299) {
                                notice(activity, "Approve the matching phone in NexusBoard, then check pairing.")
                            } else {
                                forget(activity)
                                notice(activity, "Pairing request rejected. Generate a fresh code and try again.")
                            }
                        } catch (_: Exception) {
                            notice(activity, "Pairing could not be confirmed. Check pairing before requesting another code.")
                        }
                    }.start()
                }
            }
            .setNeutralButton("Check pairing") { _, _ -> check(activity, true) }
            .setNegativeButton("Close", null)
            .show()
    }

    fun check(activity: Activity, showResult: Boolean = false) {
        val token = readCredential(activity)
        if (token == null) {
            if (showResult) notice(activity, "This phone has no NexusBoard pairing.")
            return
        }
        Thread {
            try {
                val (status, body) = request("device", token = token)
                if (showResult) {
                    val message = if (status == 200) {
                        if (JSONObject(body).optString("state") == "online") "NexusBoard device is online."
                        else "Awaiting your approval in NexusBoard."
                    } else if (status == 401) {
                        "This credential expired or was revoked. Forget it and pair again."
                    } else {
                        "NexusBoard could not check the connection."
                    }
                    notice(activity, message)
                }
            } catch (_: Exception) {
                if (showResult) notice(activity, "NexusBoard connection could not be confirmed.")
            }
        }.start()
    }

    fun forget(activity: Activity) {
        activity.getSharedPreferences(preferences, Activity.MODE_PRIVATE)
            .edit().remove("device").apply()
    }

    private fun notice(activity: Activity, message: String) {
        activity.runOnUiThread {
            if (!activity.isFinishing && !activity.isDestroyed) {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun request(action: String, payload: JSONObject? = null, token: String? = null): Pair<Int, String> {
        val connection = URL("$origin/api/connect/$action").openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"
            connection.connectTimeout = 8000
            connection.readTimeout = 10000
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val body = if (status == 200) connection.inputStream.bufferedReader().use { it.readText() } else ""
            return status to body
        } finally {
            connection.disconnect()
        }
    }

    @Synchronized private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    private fun saveCredential(activity: Activity, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        check(activity.getSharedPreferences(preferences, Activity.MODE_PRIVATE)
            .edit().putString("device", Base64.encodeToString(payload, Base64.NO_WRAP)).commit())
    }

    private fun readCredential(activity: Activity): String? {
        val stored = activity.getSharedPreferences(preferences, Activity.MODE_PRIVATE)
            .getString("device", null) ?: return null
        return runCatching {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        }.getOrNull()
    }
}
