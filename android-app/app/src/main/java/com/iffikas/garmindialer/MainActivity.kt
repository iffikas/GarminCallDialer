package com.iffikas.garmindialer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var appPermissionsButton: Button
    private lateinit var manageFavoritesButton: Button

    private val requestNotificationPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
            refreshUi()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        appPermissionsButton = findViewById(R.id.appPermissionsButton)
        manageFavoritesButton = findViewById(R.id.manageFavoritesButton)

        appPermissionsButton.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
        manageFavoritesButton.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val hasCallPermission = PermissionsActivity.hasCallPermission(this)
        // Required at runtime (not just in the manifest) on API 31+ - the
        // "connectedDevice" foreground service type refuses to start without it.
        val hasBluetoothPermission = PermissionsActivity.hasBluetoothPermission(this)

        askForNotificationsOnce()

        if (!hasCallPermission || !hasBluetoothPermission) {
            // Say which one is missing; granting happens on the App permissions screen.
            statusText.setText(
                when {
                    !hasCallPermission && !hasBluetoothPermission -> R.string.permission_required_both
                    !hasCallPermission -> R.string.permission_required
                    else -> R.string.permission_required_bluetooth
                }
            )
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

    companion object {
        private const val UI_PREFS_NAME = "ui"
        private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
    }
}
