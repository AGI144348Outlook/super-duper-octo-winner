package com.aibubble.experiment

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent

/**
 * Experimental, non-invasive text anchor locator.
 *
 * This is deliberately separate from the packaged production app. The caller
 * owns the AccessibilityService, overlay and lifecycle. No gesture injection.
 *
 * Nodes can become stale after a window transition; always re-find by text
 * rather than holding an AccessibilityNodeInfo across events.
 */
class TextAnchorTracker(
    private val onBoundsChanged: (Rect?) -> Unit
) {
    private var phrase: String? = null
    private var last: Rect? = null

    fun track(text: String) {
        phrase = text.trim().takeIf { it.isNotEmpty() }
        last = null
        onBoundsChanged(null)
    }

    fun stop() {
        phrase = null
        last = null
        onBoundsChanged(null)
    }

    /**
     * Invoke on TYPE_WINDOW_CONTENT_CHANGED, TYPE_VIEW_SCROLLED,
     * TYPE_WINDOW_STATE_CHANGED and after a target is selected.
     * The caller must debounce rapid events and call on the main thread.
     */
    fun refresh(root: AccessibilityNodeInfo?) {
        val target = phrase
        if (target == null || root == null) {
            update(null)
            return
        }
        val rect = findVisibleTextBounds(root, target)
        update(rect)
    }

    private fun update(bounds: Rect?) {
        if (last == bounds) return
        last = bounds?.let { Rect(it) }
        onBoundsChanged(bounds?.let { Rect(it) })
    }

    private fun findVisibleTextBounds(
        root: AccessibilityNodeInfo,
        query: String
    ): Rect? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited++ < 2000) {
            val node = queue.removeFirst()
            val text = node.text?.toString()
            val description = node.contentDescription?.toString()
            val matches = (text?.contains(query, ignoreCase = true) == true) ||
                (description?.contains(query, ignoreCase = true) == true)
            if (matches && node.isVisibleToUser) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                if (!bounds.isEmpty) return bounds
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    companion object {
        fun isRelevantEvent(type: Int): Boolean =
            type == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }
}
