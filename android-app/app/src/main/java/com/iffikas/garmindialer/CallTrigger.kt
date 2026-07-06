package com.iffikas.garmindialer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Fires the actual outgoing call. This is the one piece of the whole
 * project that only Android (not the watch, not Garmin Connect Mobile)
 * is able to do: hold CALL_PHONE and invoke ACTION_CALL directly, with
 * no confirmation dialog.
 */
object CallTrigger {
    private const val TAG = "CallTrigger"
    private val PHONE_PATTERN = Regex("^[+]?[0-9 ()\\-]{3,20}$")

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

        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number))).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
