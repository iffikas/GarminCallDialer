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

    @Synchronized
    private fun onSdkReadyInternal() {
        useBinderService()
        ready = true
        val callbacks = pending.toList()
        pending.clear()
        callbacks.forEach { (onReady, _) -> onReady(connectIQ) }
    }

    /**
     * By default the SDK receives watch messages through a broadcast receiver
     * registered as RECEIVER_EXPORTED with no permission and no sender check,
     * so any app on the phone could forge an INCOMING_MESSAGE and make us
     * place a call without holding CALL_PHONE itself. Switching to the binder
     * service unregisters that receiver; the binder path only accepts calls
     * from a package signed with Garmin Connect Mobile's certificate.
     *
     * Must run before any registerForAppEvents() (it clears app listeners),
     * and only once per initialize() (it unregisters the receiver, which
     * throws if done twice) - hence here rather than in the service.
     *
     * If Garmin Connect Mobile is too old to support it, the SDK leaves the
     * broadcast receiver in place, so calling still works, just unhardened.
     */
    private fun useBinderService() {
        try {
            connectIQ.registerAppToUseBinderService(Constants.WATCH_APP_ID)
            Log.d(TAG, "Using binder service for watch messages")
        } catch (e: Exception) {
            Log.w(TAG, "Binder service unavailable, falling back to broadcasts", e)
        }
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
