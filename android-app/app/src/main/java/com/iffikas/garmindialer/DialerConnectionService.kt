package com.iffikas.garmindialer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException

/**
 * Stays alive in the foreground so the ConnectIQ SDK connection to the
 * watch survives Android's background/Doze restrictions. Without a
 * foreground service (or a battery-optimization exemption), Android will
 * kill this listener while the phone is locked - the single biggest
 * real-world reliability risk for this whole setup.
 */
class DialerConnectionService : Service() {

    private lateinit var connectIQ: ConnectIQ
    private val watchApp = IQApp(Constants.WATCH_APP_ID)
    private var sdkReady = false

    override fun onCreate() {
        super.onCreate()
        startForeground(Constants.NOTIFICATION_ID, buildNotification(getString(R.string.status_starting)))

        connectIQ = ConnectIQ.getInstance(applicationContext, ConnectIQ.IQConnectType.WIRELESS)
        connectIQ.initialize(applicationContext, true, object : ConnectIQ.ConnectIQListener {
            override fun onSdkReady() {
                sdkReady = true
                registerForWatchEvents()
            }

            override fun onInitializeError(status: ConnectIQ.IQSdkErrorStatus) {
                Log.e(TAG, "ConnectIQ init error: $status")
                updateNotification(getString(R.string.status_connect_unavailable))
            }

            override fun onSdkShutDown() {
                sdkReady = false
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Sticky: if Android kills the process, restart the service (and
        // re-establish the watch connection) as soon as resources allow.
        return START_STICKY
    }

    private fun registerForWatchEvents() {
        try {
            val devices = connectIQ.knownDevices ?: emptyList()
            if (devices.isEmpty()) {
                updateNotification(getString(R.string.status_no_device))
                return
            }

            for (device in devices) {
                connectIQ.registerForDeviceEvents(device) { _, status ->
                    Log.d(TAG, "Device status changed: $status")
                }
                connectIQ.registerForAppEvents(device, watchApp) { _, _, message, _ ->
                    handleMessage(message)
                }
            }
            updateNotification(getString(R.string.status_watching))
        } catch (e: InvalidStateException) {
            Log.e(TAG, "ConnectIQ not initialized", e)
        } catch (e: ServiceUnavailableException) {
            Log.e(TAG, "Garmin Connect Mobile not running", e)
            updateNotification(getString(R.string.status_connect_unavailable))
        }
    }

    private fun handleMessage(message: List<Any>?) {
        val payload = message?.getOrNull(0) as? Map<*, *> ?: return
        val number = payload["n"] as? String ?: return
        CallTrigger.placeCall(applicationContext, number)
    }

    override fun onDestroy() {
        if (sdkReady) {
            try {
                connectIQ.unregisterAllForEvents()
                connectIQ.shutdown(applicationContext)
            } catch (e: InvalidStateException) {
                Log.e(TAG, "Error shutting down ConnectIQ", e)
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                "Dialer connection",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.sym_call_outgoing)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(Constants.NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        private const val TAG = "DialerConnectionService"
    }
}
