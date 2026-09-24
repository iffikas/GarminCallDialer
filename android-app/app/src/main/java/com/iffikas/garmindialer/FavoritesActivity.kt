package com.iffikas.garmindialer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException

/**
 * Lets the user define up to [Constants.MAX_FAVORITES] name/number
 * favorites - either picked from the phone's contacts or typed in by hand
 * (for things like gate/intercom codes that aren't in Contacts) - and push
 * them to the watch over the same ConnectIQ BLE channel the watch uses to
 * send call requests, in the opposite direction.
 *
 * The picker uses ContactsContract.CommonDataKinds.Phone.CONTENT_URI via
 * ACTION_PICK rather than querying Contacts directly, so no READ_CONTACTS
 * permission is needed - the system picker grants a one-off read URI for
 * just the row the user selected.
 */
class FavoritesActivity : AppCompatActivity() {

    private lateinit var nameFields: Array<EditText>
    private lateinit var numberFields: Array<EditText>
    private lateinit var syncStatusText: TextView
    private lateinit var prefs: android.content.SharedPreferences

    private var pickContactForIndex = -1
    private val pickContact = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onContactPicked(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        title = getString(R.string.manage_favorites)

        prefs = getSharedPreferences(Constants.FAVORITES_PREFS_NAME, MODE_PRIVATE)
        syncStatusText = findViewById(R.id.syncStatusText)

        val container = findViewById<LinearLayout>(R.id.favoritesContainer)
        val inflater = LayoutInflater.from(this)
        nameFields = Array(Constants.MAX_FAVORITES) { EditText(this) }
        numberFields = Array(Constants.MAX_FAVORITES) { EditText(this) }

        for (i in 0 until Constants.MAX_FAVORITES) {
            val row = inflater.inflate(R.layout.item_favorite_row, container, false)
            nameFields[i] = row.findViewById(R.id.nameField)
            numberFields[i] = row.findViewById(R.id.numberField)
            nameFields[i].setText(prefs.getString(Constants.favoriteNameKey(i), ""))
            numberFields[i].setText(prefs.getString(Constants.favoriteNumberKey(i), ""))

            row.findViewById<Button>(R.id.pickContactButton).setOnClickListener {
                pickContactForIndex = i
                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                pickContact.launch(intent)
            }
            row.findViewById<Button>(R.id.clearButton).setOnClickListener {
                nameFields[i].setText("")
                numberFields[i].setText("")
            }

            container.addView(row)
        }

        findViewById<Button>(R.id.sendToWatchButton).setOnClickListener {
            saveFavorites()
            sendFavoritesToWatch()
        }
    }

    private fun onContactPicked(result: androidx.activity.result.ActivityResult) {
        val index = pickContactForIndex
        pickContactForIndex = -1
        if (result.resultCode != Activity.RESULT_OK || index !in 0 until Constants.MAX_FAVORITES) {
            return
        }
        val contactUri: Uri = result.data?.data ?: return

        contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                val number = if (numberIndex >= 0) cursor.getString(numberIndex) else null

                if (!name.isNullOrBlank()) {
                    nameFields[index].setText(name)
                }
                if (!number.isNullOrBlank()) {
                    numberFields[index].setText(number)
                }
            }
        }
    }

    private fun collectFavorites(): List<Map<String, String>> {
        val favorites = mutableListOf<Map<String, String>>()
        for (i in 0 until Constants.MAX_FAVORITES) {
            val name = nameFields[i].text.toString().trim()
            val number = numberFields[i].text.toString().trim()
            if (name.isNotEmpty() && number.isNotEmpty()) {
                favorites.add(mapOf("name" to name, "number" to number))
            }
        }
        return favorites
    }

    private fun saveFavorites() {
        prefs.edit().apply {
            for (i in 0 until Constants.MAX_FAVORITES) {
                putString(Constants.favoriteNameKey(i), nameFields[i].text.toString().trim())
                putString(Constants.favoriteNumberKey(i), numberFields[i].text.toString().trim())
            }
        }.apply()
    }

    private fun sendFavoritesToWatch() {
        val favorites = collectFavorites()
        if (favorites.isEmpty()) {
            syncStatusText.text = getString(R.string.sync_no_favorites)
            return
        }

        val connectIQ = ConnectIQ.getInstance(applicationContext, ConnectIQ.IQConnectType.WIRELESS)
        connectIQ.initialize(applicationContext, true, object : ConnectIQ.ConnectIQListener {
            override fun onSdkReady() {
                sendPayload(connectIQ, favorites)
            }

            override fun onInitializeError(status: ConnectIQ.IQSdkErrorStatus) {
                Log.e(TAG, "ConnectIQ init error: $status")
                syncStatusText.text = getString(R.string.sync_connect_unavailable)
            }

            override fun onSdkShutDown() {}
        })
    }

    private fun sendPayload(connectIQ: ConnectIQ, favorites: List<Map<String, String>>) {
        try {
            val devices: List<IQDevice> = connectIQ.knownDevices ?: emptyList()
            if (devices.isEmpty()) {
                syncStatusText.text = getString(R.string.sync_no_device)
                return
            }

            val watchApp = IQApp(Constants.WATCH_APP_ID)
            val payload = mapOf("favorites" to favorites)
            for (device in devices) {
                connectIQ.sendMessage(device, watchApp, payload) { _, _, status ->
                    runOnUiThread {
                        syncStatusText.text = if (status == ConnectIQ.IQMessageStatus.SUCCESS) {
                            getString(R.string.sync_success)
                        } else {
                            getString(R.string.sync_failed, status.toString())
                        }
                    }
                }
            }
        } catch (e: InvalidStateException) {
            Log.e(TAG, "ConnectIQ not initialized", e)
            syncStatusText.text = getString(R.string.sync_connect_unavailable)
        } catch (e: ServiceUnavailableException) {
            Log.e(TAG, "Garmin Connect Mobile not running", e)
            syncStatusText.text = getString(R.string.sync_connect_unavailable)
        }
    }

    companion object {
        private const val TAG = "FavoritesActivity"
    }
}
