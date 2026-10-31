package com.iffikas.garmindialer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var grantCallPermissionButton: Button
    private lateinit var grantBluetoothPermissionButton: Button
    private lateinit var disableBatteryOptimizationButton: Button
    private lateinit var manageFavoritesButton: Button

    private val requestCallPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
            refreshUi()
        }
    private val requestBluetoothPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
            refreshUi()
        }
    private val requestNotificationPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
            refreshUi()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        grantCallPermissionButton = findViewById(R.id.grantCallPermissionButton)
        grantBluetoothPermissionButton = findViewById(R.id.grantBluetoothPermissionButton)
        disableBatteryOptimizationButton = findViewById(R.id.disableBatteryOptimizationButton)
        manageFavoritesButton = findViewById(R.id.manageFavoritesButton)

        manageFavoritesButton.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        grantCallPermissionButton.setOnClickListener {
            requestCallPermission.launch(Manifest.permission.CALL_PHONE)
        }
        grantBluetoothPermissionButton.setOnClickListener {
            requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
        disableBatteryOptimizationButton.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val hasCallPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        // Required at runtime (not just in the manifest) on API 31+ - the
        // "connectedDevice" foreground service type refuses to start without it.
        val hasBluetoothPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

        askForNotificationsOnce()

        grantCallPermissionButton.visibility = if (hasCallPermission) android.view.View.GONE else android.view.View.VISIBLE
        grantBluetoothPermissionButton.visibility = if (hasBluetoothPermission) android.view.View.GONE else android.view.View.VISIBLE

        val batteryOptimized = !isIgnoringBatteryOptimizations()
        disableBatteryOptimizationButton.visibility =
            if (batteryOptimized) android.view.View.VISIBLE else android.view.View.GONE

        if (!hasCallPermission || !hasBluetoothPermission) {
            statusText.setText(R.string.permission_required)
            return
        }

        val serviceIntent = Intent(this, DialerConnectionService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        statusText.setText(R.string.status_watching)
    }

    // Notifications are optional: without them the service still runs and
    // calls still go through, Android just hides the "Watching for call
    // requests" notification. So ask once, on first launch, and never again -
    // asking on every resume nagged anyone who had said no.
    private fun askForNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val prefs = getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
        if (granted || prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)) {
            return
        }
        prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    @Suppress("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    companion object {
        private const val UI_PREFS_NAME = "ui"
        private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
    }
}
