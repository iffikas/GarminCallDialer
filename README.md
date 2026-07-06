# Garmin Call Dialer

Trigger a phone call directly from a Garmin Instinct 2 Solar, no phone interaction required.

Connect IQ has no native "place a phone call" API, so this is two apps talking over BLE:

```
 Garmin watch (Connect IQ widget)          Android phone (companion app)
 ┌─────────────────────────────┐  BLE via  ┌───────────────────────────────┐
 │ Favorites menu (5 slots,    │  Garmin   │ ConnectIQ SDK listener        │
 │ Name+Number set via Garmin  │  Connect  │ (foreground service)          │
 │ Connect Mobile app settings)│  Mobile   │        │                      │
 │        │                    │ ────────► │        ▼                     │
 │        ▼                    │           │ Intent.ACTION_CALL            │
 │ Communications.transmit()   │           │ (CALL_PHONE permission)       │
 └─────────────────────────────┘           └───────────────────────────────┘
```

- **watch-app/** — Connect IQ widget (Monkey C). Shows up to 5 favorites, transmits `{"n": "<number>"}` on selection.
- **android-app/** — Android companion app (Kotlin). Receives the message via the Connect IQ Mobile SDK and places the call directly, no confirmation dialog.

Personal use only — both apps are sideloaded. Neither is submitted to the Connect IQ Store or Google Play.

## Prerequisites

- Garmin Connect Mobile installed on the phone, with the Instinct 2 Solar already paired.
- **Watch side**: [Connect IQ SDK Manager](https://developer.garmin.com/connect-iq/sdk/) + the "Monkey C" VS Code extension, with the Instinct 2 Solar device downloaded in the SDK Manager.
- **Android side**: Android Studio (or just the command-line SDK) with `ANDROID_HOME` set, and a JDK 17 (`JAVA_HOME` — the Android Studio bundled JBR works: `.../Android Studio/jbr`).
- A Connect IQ developer signing key at `keys/developer_key.der` (`keys/developer_key.pem` for the private key). Generate one via the Monkey C extension ("Garmin: Generate Developer Key") if `keys/` is empty — it's gitignored and never committed.

## Building & installing the watch app

```bash
cd watch-app
monkeyc -f monkey.jungle -y ../keys/developer_key.der -d instinct2solar -o bin/DialerApp.prg -w
```

- **Simulator**: run the Connect IQ simulator (`connectiq`), then `monkeydo bin/DialerApp.prg instinct2solar`.
- **Real watch**: connect the watch over USB, mount as a drive, and copy `bin/DialerApp.prg` into `GARMIN/APPS/`. Eject and the widget appears in the watch's widget glances.

**Setting favorites**: open Garmin Connect Mobile → Watch → look for "Garmin Call Dialer" under app settings → fill in Name/Number for up to 5 slots. The widget only lists slots where both fields are set.

> **Simulator gotcha**: the simulator persists Application Settings to disk across reloads just like a real device. If you change a default in `resources/settings` or `resources/properties`, a previous run's stored value will win until you edit it through the simulator's Settings menu (or erase the simulator's stored data).

## Building & installing the Android app

```bash
cd android-app
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME="$HOME/AppData/Local/Android/Sdk" ./gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

Install on the phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On first launch:
1. Tap **Grant call permission** (`CALL_PHONE`) — required, the app can't dial without it.
2. Tap **Disable battery optimization** — without this, Android may kill the background connection while the phone is locked, which is the single biggest real-world reliability risk here.
3. Accept the notification permission prompt (Android 13+) — the app runs a low-priority foreground notification ("Watching for watch...") so Android doesn't kill the listener.

The app has no other UI — once permissions are granted it silently starts a foreground service that listens for messages from the watch app (matched by the app ID in `watch-app/manifest.xml` / `Constants.WATCH_APP_ID`) and calls `Intent.ACTION_CALL` on anything that looks like a phone number.

## Testing the end-to-end flow

1. Confirm Garmin Connect Mobile shows the watch as connected.
2. Open the companion app on the phone once so its foreground service is running (check the notification shade for "Watching for watch...").
3. On the watch, open the Garmin Call Dialer widget and select a favorite.
4. Watch should show "Sending..." then return to the menu (or show a failure message if BLE delivery failed).
5. Phone should immediately start a call to that number — no confirmation prompt.

Troubleshooting:
- **Widget shows "No favorites"**: none of the 5 slots have both Name and Number set — check Garmin Connect Mobile app settings, and see the simulator gotcha above if testing in-simulator.
- **Notification says "Garmin Connect Mobile not running"**: launch Garmin Connect Mobile on the phone first: the ConnectIQ SDK proxies through it.
- **Notification says "No device"**: the watch isn't in the phone's list of known Connect IQ devices yet — open Garmin Connect Mobile and let it finish syncing with the watch at least once.
- **Nothing happens on the phone at all**: check `adb logcat -s DialerConnectionService CallTrigger` while triggering from the watch.

## Known limitations (v1)

- No call state feedback to the watch — "Sending" only confirms BLE delivery, not that the call connected.
- Phone number validation on the Android side is a permissive regex (digits, spaces, `+`, `-`, parens, 3–20 chars) — good enough to reject garbage, not a full E.164 validator.
- Up to 5 favorites, fixed slots (no add/remove, just fill in the ones you want).
