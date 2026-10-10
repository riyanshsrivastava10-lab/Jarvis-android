package com.riyansh.jarvis

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import org.json.JSONObject

// ⚙️ MECHANICAL HAND AUTOMATION MODULE (Tapping, Scrolling, Navigation)
class JarvisAutomationService : AccessibilityService() {

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val rawToken = intent?.getStringExtra("raw_token") ?: return
            
            // Can handle plain action strings or JSON commands
            var actionType = rawToken.trim()
            var target = ""

            try {
                if (rawToken.startsWith("{")) {
                    val json = JSONObject(rawToken)
                    actionType = json.optString("action", actionType)
                    target = json.optString("target", "")
                }
            } catch (e: Exception) {
                // Treat as direct action string
            }

            executeDeviceInput(actionType, target)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter("JARVIS_EXECUTE_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(actionReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Hook to read window contents if needed
    }

    override fun onInterrupt() {}

    fun executeDeviceInput(actionType: String, target: String = "") {
        when (actionType) {
            "SCROLL_DOWN" -> {
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels.toFloat()
                val screenHeight = displayMetrics.heightPixels.toFloat()

                val path = Path().apply {
                    moveTo(screenWidth / 2f, screenHeight * 0.75f) // Near bottom
                    lineTo(screenWidth / 2f, screenHeight * 0.25f) // Drag up to scroll down
                }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
                    .build()
                dispatchGesture(gesture, null, null)
            }
            "OPEN_APP" -> {
                val pkgName = if (target.isNotEmpty()) target else "com.google.android.youtube"
                val launchIntent = packageManager.getLaunchIntentForPackage(pkgName)
                launchIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                }
            }
            "NOTIFICATION_PANEL" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "GLOBAL_HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "GLOBAL_BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "GLOBAL_RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(actionReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}