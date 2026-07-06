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
    private lateinit var disableBatteryOptimizationButton: Button

    private val requestCallPermission =
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
        disableBatteryOptimizationButton = findViewById(R.id.disableBatteryOptimizationButton)

        grantCallPermissionButton.setOnClickListener {
            requestCallPermission.launch(Manifest.permission.CALL_PHONE)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        grantCallPermissionButton.visibility = if (hasCallPermission) android.view.View.GONE else android.view.View.VISIBLE

        val batteryOptimized = !isIgnoringBatteryOptimizations()
        disableBatteryOptimizationButton.visibility =
            if (batteryOptimized) android.view.View.VISIBLE else android.view.View.GONE

        if (!hasCallPermission) {
            statusText.setText(R.string.permission_required)
            return
        }

        if (hasCallPermission) {
            val serviceIntent = Intent(this, DialerConnectionService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
            statusText.setText(R.string.status_watching)
        }
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
}
