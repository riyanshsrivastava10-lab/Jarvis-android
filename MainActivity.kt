package com.riyansh.jarvis

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var accessibilityStatusText: TextView
    private lateinit var btnToggleEar: Button
    private lateinit var btnOpenAccessibility: Button
    private var isEarRunning = false

    companion object {
        private const val PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        accessibilityStatusText = findViewById(R.id.accessibilityStatusText)
        btnToggleEar = findViewById(R.id.btnToggleEar)
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility)

        checkAndRequestPermissions()

        btnOpenAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnToggleEar.setOnClickListener {
            if (!hasAudioPermission()) {
                checkAndRequestPermissions()
                return@setOnClickListener
            }
            if (isEarRunning) {
                stopEarService()
            } else {
                startEarService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityStatus()
    }

    private fun updateAccessibilityStatus() {
        val isEnabled = isAccessibilityServiceEnabled(JarvisAutomationService::class.java)
        if (isEnabled) {
            accessibilityStatusText.text = "Mechanical Hand: ACTIVE (Enabled)"
            accessibilityStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            accessibilityStatusText.text = "Mechanical Hand: DISABLED (Tap to enable)"
            accessibilityStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }

    private fun isAccessibilityServiceEnabled(serviceClass: Class<*>): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.name == serviceClass.name) {
                return true
            }
        }
        return false
    }

    private fun startEarService() {
        val intent = Intent(this, SilentJarvisEngine::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isEarRunning = true
        btnToggleEar.text = "Stop Jarvis Ear"
        statusText.text = "Status: Continuous Ear Listening..."
    }

    private fun stopEarService() {
        val intent = Intent(this, SilentJarvisEngine::class.java)
        stopService(intent)
        isEarRunning = false
        btnToggleEar.text = "Start Jarvis Ear"
        statusText.text = "Status: Idle"
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }
}