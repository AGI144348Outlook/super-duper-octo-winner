package com.aibubble.experiment

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Experimental Hebrew sight/sound controls for an existing floating overlay.
 *
 * ע = sight (screen capture consent and user-triggered screen refresh)
 * פ = sound (independent voice session)
 *
 * The Android host must supply its own overlay window, lifecycle and permission
 * handling. These controls do not start MediaProjection or microphone recording.
 */
class HebrewSessionControls(
    context: Context,
    private val actions: Actions
) : LinearLayout(context) {
    interface Actions {
        /** Host must obtain user consent before enabling capture. */
        fun onSightToggleRequested()
        /** One user-initiated observation request, never passive monitoring. */
        fun onSightRefreshRequested()
        fun onSoundToggleRequested()
    }

    private val sight = makeButton("ע", "Sight")
    private val sound = makeButton("פ", "Sound")
    private val status = TextView(context).apply {
        textSize = 11f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        text = "Sight off · Sound off"
        contentDescription = "Sight off, Sound off"
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(6), dp(8), dp(6))
        background = GradientDrawable().apply {
            setColor(0xBB20242A.toInt())
            cornerRadius = dp(18).toFloat()
        }
        val buttons = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        buttons.addView(sight, LayoutParams(dp(64), dp(64)))
        buttons.addView(sound, LayoutParams(dp(64), dp(64)))
        addView(buttons)
        addView(status)

        // Tap ע to request one fresh observation when the sight session is on.
        // Long-press ע to start/stop the consent-gated screen session.
        sight.setOnClickListener { actions.onSightRefreshRequested() }
        sight.setOnLongClickListener {
            actions.onSightToggleRequested()
            true
        }
        sound.setOnClickListener { actions.onSoundToggleRequested() }
    }

    fun render(sightOn: Boolean, soundOn: Boolean) {
        style(sight, sightOn, 0xFF2B8D9B.toInt())
        style(sound, soundOn, 0xFFAA6BBA.toInt())
        sight.contentDescription =
            if (sightOn) "Sight on. Tap to update screen, hold to stop."
            else "Sight off. Hold to request screen permission."
        sound.contentDescription =
            if (soundOn) "Sound on. Tap to turn off."
            else "Sound off. Tap to turn on."
        status.text = "Sight ${if (sightOn) "on" else "off"} · Sound ${if (soundOn) "on" else "off"}"
        status.contentDescription = status.text
    }

    private fun makeButton(glyph: String, label: String): TextView =
        TextView(context).apply {
            text = glyph
            textSize = 33f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            contentDescription = label
            isClickable = true
            isFocusable = true
        }

    private fun style(view: View, active: Boolean, activeColor: Int) {
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (active) activeColor else 0xFF444850.toInt())
            setStroke(dp(2), if (active) Color.WHITE else 0xFF777B82.toInt())
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
