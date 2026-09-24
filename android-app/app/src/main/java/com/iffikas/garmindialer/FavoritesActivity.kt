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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
 * Only the favorites that exist are shown as rows; "Add favorite" appends a
 * new empty row (up to the limit) and each row can be removed individually,
 * rather than always showing 5 blank slots.
 *
 * The picker uses ContactsContract.CommonDataKinds.Phone.CONTENT_URI via
 * ACTION_PICK rather than querying Contacts directly, so no READ_CONTACTS
 * permission is needed - the system picker grants a one-off read URI for
 * just the row the user selected.
 */
class FavoritesActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var addFavoriteButton: Button
    private lateinit var syncStatusText: TextView

    private var pendingPickRow: android.view.View? = null
    private val pickContact = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onContactPicked(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        title = getString(R.string.manage_favorites)

        container = findViewById(R.id.favoritesContainer)
        addFavoriteButton = findViewById(R.id.addFavoriteButton)
        syncStatusText = findViewById(R.id.syncStatusText)

        for (favorite in FavoritesRepository.load(this)) {
            addFavoriteRow(favorite["name"] ?: "", favorite["number"] ?: "")
        }

        addFavoriteButton.setOnClickListener {
            addFavoriteRow("", "")
        }
        updateAddButtonVisibility()

        findViewById<Button>(R.id.sendToWatchButton).setOnClickListener {
            saveFavorites()
            sendFavoritesToWatch()
        }

        // Android 15+ (targetSdk 36) draws edge-to-edge by default, so the
        // bottom bar needs its own padding to clear the gesture/navigation
        // bar instead of being partially hidden behind it.
        val bottomBar = findViewById<LinearLayout>(R.id.bottomBar)
        val bottomBarBasePadding = bottomBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(bottomBar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomBarBasePadding + systemBars.bottom)
            insets
        }
    }

    private fun addFavoriteRow(name: String, number: String) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_favorite_row, container, false)
        row.findViewById<EditText>(R.id.nameField).setText(name)
        row.findViewById<EditText>(R.id.numberField).setText(number)

        row.findViewById<Button>(R.id.pickContactButton).setOnClickListener {
            pendingPickRow = row
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContact.launch(intent)
        }
        row.findViewById<Button>(R.id.removeButton).setOnClickListener {
            container.removeView(row)
            updateAddButtonVisibility()
        }

        container.addView(row)
        updateAddButtonVisibility()
    }

    private fun updateAddButtonVisibility() {
        addFavoriteButton.visibility =
            if (container.childCount >= Constants.MAX_FAVORITES) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun onContactPicked(result: androidx.activity.result.ActivityResult) {
        val row = pendingPickRow
        pendingPickRow = null
        if (result.resultCode != Activity.RESULT_OK || row == null) {
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
                    row.findViewById<EditText>(R.id.nameField).setText(name)
                }
                if (!number.isNullOrBlank()) {
                    row.findViewById<EditText>(R.id.numberField).setText(number)
                }
            }
        }
    }

    private fun collectFavorites(): List<Map<String, String>> {
        val favorites = mutableListOf<Map<String, String>>()
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i)
            val name = row.findViewById<EditText>(R.id.nameField).text.toString().trim()
            val number = row.findViewById<EditText>(R.id.numberField).text.toString().trim()
            if (name.isNotEmpty() && number.isNotEmpty()) {
                favorites.add(mapOf("name" to name, "number" to number))
            }
        }
        return favorites
    }

    private fun saveFavorites() {
        FavoritesRepository.save(this, collectFavorites())
    }

    private fun sendFavoritesToWatch() {
        val favorites = collectFavorites()
        if (favorites.isEmpty()) {
            syncStatusText.text = getString(R.string.sync_no_favorites)
            return
        }

        // Give immediate feedback - the SDK connect + send below is async
        // and can take a couple of seconds, and a silent button was the
        // original complaint that led here.
        syncStatusText.text = getString(R.string.sync_connecting)

        ConnectIqManager.runWhenReady(
            applicationContext,
            onReady = { connectIQ -> sendPayload(connectIQ, favorites) },
            onError = { status ->
                Log.e(TAG, "ConnectIQ init error: $status")
                runOnUiThread {
                    syncStatusText.text = getString(R.string.sync_connect_unavailable)
                }
            }
        )
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
