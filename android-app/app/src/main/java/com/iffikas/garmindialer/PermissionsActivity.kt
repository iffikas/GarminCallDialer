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
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Shows every permission the app uses, whether it's currently allowed, and
 * a button to change it. Unlike a setup screen the button never disappears:
 * "Allow" while missing, "Manage" once granted, so the user can also take a
 * permission away again from here.
 *
 * Revoking a permission in Android settings makes Android kill the process
 * ("Killing <pid>:com.iffikas.garmindialer ... permissions revoked") - that's
 * normal and not a crash; the foreground service is restarted by the system.
 */
class PermissionsActivity : AppCompatActivity() {

    // The permission the last runtime request was for, so the result
    // callback can tell whether Android even showed the dialog.
    private var pendingPermission: String? = null

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val permission = pendingPermission
        pendingPermission = null
        // Denied without a rationale on a permission we've asked for before
        // means "don't ask again": Android no longer shows the dialog, so
        // the only way left is the app's settings page. (On the very first
        // request a denial without rationale can also just be a dismissed
        // dialog, hence the "asked before" check.)
        if (!granted && permission != null && wasAskedBefore(permission) &&
            !shouldShowRequestPermissionRationale(permission)
        ) {
            openAppDetailsSettings()
        }
        if (permission != null) {
            markAsked(permission)
        }
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        title = getString(R.string.app_permissions)

        // targetSdk 35+ draws edge-to-edge; keep the content clear of the
        // status and navigation bars.
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.callButton).setOnClickListener {
            if (hasCallPermission(this)) {
                openAppDetailsSettings()
            } else {
                launchRequest(Manifest.permission.CALL_PHONE)
            }
        }
        findViewById<Button>(R.id.bluetoothButton).setOnClickListener {
            if (hasBluetoothPermission(this) || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                openAppDetailsSettings()
            } else {
                launchRequest(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        findViewById<Button>(R.id.notificationsButton).setOnClickListener {
            if (hasNotificationPermission(this) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                openNotificationSettings()
            } else {
                launchRequest(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        findViewById<Button>(R.id.batteryButton).setOnClickListener {
            if (isIgnoringBatteryOptimizations(this)) {
                // The per-app toggle isn't reachable directly; this opens the
                // system list where the user can switch it back to "optimised".
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } else {
                @Suppress("BatteryLife")
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val call = hasCallPermission(this)
        val bluetooth = hasBluetoothPermission(this)
        bind(R.id.callState, R.id.callButton, call)
        bind(R.id.bluetoothState, R.id.bluetoothButton, bluetooth)
        bind(R.id.notificationsState, R.id.notificationsButton, hasNotificationPermission(this))
        bind(R.id.batteryState, R.id.batteryButton, isIgnoringBatteryOptimizations(this))

        // Same as MainActivity: the service is only startable once both
        // required permissions are in place; (re)starting it is a no-op if
        // it's already running.
        if (call && bluetooth) {
            ContextCompat.startForegroundService(this, Intent(this, DialerConnectionService::class.java))
        }
    }

    private fun bind(stateId: Int, buttonId: Int, granted: Boolean) {
        findViewById<TextView>(stateId).apply {
            setText(if (granted) R.string.permission_granted else R.string.permission_missing)
            setTextColor(getColor(if (granted) R.color.ok else R.color.warn))
        }
        findViewById<Button>(buttonId).setText(if (granted) R.string.permission_manage else R.string.permission_allow)
    }

    private fun launchRequest(permission: String) {
        pendingPermission = permission
        requestPermission.launch(permission)
    }

    private fun wasAskedBefore(permission: String): Boolean =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ASKED_PREFIX + permission, false)

    private fun markAsked(permission: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ASKED_PREFIX + permission, true)
            .apply()
    }

    private fun openAppDetailsSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }

    companion object {
        private const val PREFS_NAME = "permissions"
        private const val KEY_ASKED_PREFIX = "asked_"

        fun hasCallPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED

        /**
         * Required at runtime (not just in the manifest) on API 31+ - the
         * "connectedDevice" foreground service type refuses to start without
         * it. Older versions grant Bluetooth at install time.
         */
        fun hasBluetoothPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED

        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        fun isIgnoringBatteryOptimizations(context: Context): Boolean =
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
    }
}
