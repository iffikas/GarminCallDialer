package com.iffikas.garmindialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Fires the actual outgoing call. This is the one piece of the whole
 * project that only Android (not the watch, not Garmin Connect Mobile)
 * is able to do: hold CALL_PHONE and place the call directly, with no
 * confirmation dialog.
 *
 * Uses TelecomManager.placeCall() rather than starting an ACTION_CALL
 * activity directly - starting an activity from this background service
 * gets silently blocked by Android's Background Activity Launch
 * restrictions (no crash, no call). TelecomManager routes the request
 * through the system Telecom service instead, which brings up the
 * in-call UI itself and isn't subject to our process's BAL state.
 */
object CallTrigger {
    private const val TAG = "CallTrigger"
    private val PHONE_PATTERN = Regex("^[+]?[0-9 ()\\-*#]{3,20}$")

    fun placeCall(context: Context, rawNumber: String) {
        val number = rawNumber.trim()

        if (!PHONE_PATTERN.matches(number)) {
            Log.w(TAG, "Rejected implausible number from watch: $number")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "CALL_PHONE permission not granted, cannot place call")
            return
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        telecomManager.placeCall(Uri.parse("tel:" + Uri.encode(number)), null)
        Log.d(TAG, "TelecomManager.placeCall invoked for: $number")
    }
}
