package com.iffikas.garmindialer

import android.content.Context
import android.util.Log
import com.garmin.android.connectiq.ConnectIQ

/**
 * Single shared entry point to the ConnectIQ Mobile SDK.
 *
 * ConnectIQ.getInstance() is a process-wide singleton, but DialerConnectionService
 * (the long-running background call listener) and FavoritesActivity (favorites
 * sync, opened on demand) both need it. Each independently calling
 * connectIQ.initialize() with its own listener is the likely cause of
 * "Send to watch" silently doing nothing while the service is already
 * running: the second initialize() call races with (or is ignored in favor
 * of) the first, so the second listener's onSdkReady/onInitializeError never
 * fires. Routing every caller through here means initialize() only ever
 * runs once per process, and late callers get an immediate answer if the
 * SDK is already ready.
 */
object ConnectIqManager {
    private const val TAG = "ConnectIqManager"

    lateinit var connectIQ: ConnectIQ
        private set

    private var initStarted = false
    private var ready = false
    private val pending = mutableListOf<Pair<(ConnectIQ) -> Unit, (ConnectIQ.IQSdkErrorStatus) -> Unit>>()

    @Synchronized
    fun runWhenReady(
        context: Context,
        onReady: (ConnectIQ) -> Unit,
        onError: (ConnectIQ.IQSdkErrorStatus) -> Unit = {}
    ) {
        if (ready) {
            onReady(connectIQ)
            return
        }
        pending.add(onReady to onError)
        ensureInitialized(context)
    }

    @Synchronized
    private fun ensureInitialized(context: Context) {
        if (initStarted) {
            return
        }
        initStarted = true

        val appContext = context.applicationContext
        connectIQ = ConnectIQ.getInstance(appContext, ConnectIQ.IQConnectType.WIRELESS)
        connectIQ.initialize(appContext, true, object : ConnectIQ.ConnectIQListener {
            override fun onSdkReady() {
                Log.d(TAG, "SDK ready")
                onSdkReadyInternal()
            }

            override fun onInitializeError(status: ConnectIQ.IQSdkErrorStatus) {
                Log.e(TAG, "SDK init error: $status")
                onSdkErrorInternal(status)
            }

            override fun onSdkShutDown() {
                Log.d(TAG, "SDK shut down")
                ready = false
                initStarted = false
            }
        })
    }

    // Deliberately NOT calling connectIQ.registerAppToUseBinderService().
    // It would close the SDK's exported broadcast receiver (which has no
    // sender check) in favour of a signature-checked binder service, but
    // Garmin Connect Mobile 5.30.1 accepts the registration and then never
    // binds to that service: watch messages were silently dropped and no
    // call was ever placed (verified on a real phone, 2026-10-31). The
    // forged-broadcast risk is instead limited in DialerConnectionService,
    // which only dials numbers that are in the user's own favorites.
    @Synchronized
    private fun onSdkReadyInternal() {
        ready = true
        val callbacks = pending.toList()
        pending.clear()
        callbacks.forEach { (onReady, _) -> onReady(connectIQ) }
    }

    @Synchronized
    private fun onSdkErrorInternal(status: ConnectIQ.IQSdkErrorStatus) {
        ready = false
        initStarted = false
        val callbacks = pending.toList()
        pending.clear()
        callbacks.forEach { (_, onError) -> onError(status) }
    }
}
