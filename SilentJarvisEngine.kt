package com.riyansh.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SilentJarvisEngine : Service() {

    private var audioRecord: AudioRecord? = null
    private var isRunning = false
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    companion object {
        const val CHANNEL_ID = "jarvis_continuous_ear"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.riyansh.jarvis.STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundNotification()
        connectToAiServer()
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Background Ear",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Listening for audio commands"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis Active")
            .setContentText("Continuous Ear is listening...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun connectToAiServer() {
        // Tip: On Android emulator use 10.0.2.2:8080, on physical phone use PC's Wi-Fi IP (e.g. 192.168.1.X:8080)
        val serverUrl = "ws://10.0.2.2:8080"
        val request = Request.Builder().url(serverUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                startAudioStreaming()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseAndForwardAction(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                t.printStackTrace()
            }
        })
    }

    private fun startAudioStreaming() {
        Thread {
            val sampleRate = 16000
            val config = AudioFormat.CHANNEL_IN_MONO
            val format = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, config, format)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    config,
                    format,
                    bufferSize
                )
                audioRecord?.startRecording()

                val buffer = ByteArray(bufferSize)
                while (isRunning) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Streams PCM chunk in base64
                        val audioByteString = ByteString.of(buffer, 0, read)
                        val audioJson = JSONObject()
                        val inputObj = JSONObject()
                        inputObj.put("mediaChunks", audioByteString.base64())
                        audioJson.put("realtimeInput", inputObj)
                        webSocket?.send(audioJson.toString())
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun parseAndForwardAction(rawResponse: String) {
        val intent = Intent("JARVIS_EXECUTE_ACTION").apply {
            putExtra("raw_token", rawResponse)
            setPackage(packageName) // Explicit broadcast for security
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        webSocket?.close(1000, "Service Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}