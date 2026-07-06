# Garmin Call Dialer

Trigger a phone call directly from a Garmin Instinct 2 Solar, no phone interaction required.

## Why I built this

I use a gate/intercom code to get into my building, and I got tired of digging my phone out just to dial it. My Garmin Instinct 2 Solar seemed like the obvious way to do this with one press from my wrist — except Garmin's Connect IQ platform has no built-in way to place a phone call. This is the result of solving that for myself: a watch widget paired with a small Android companion app that does the actual dialing.

It's built entirely around my own use case (a handful of speed-dial-style favorites, sideloaded on my own devices), so treat it as a working example/starting point rather than a polished product — see [Known limitations](#known-limitations-v1).

Connect IQ has no native "place a phone call" API, so this is two apps talking over BLE:

```
 Garmin watch (Connect IQ widget)          Android phone (companion app)
 ┌─────────────────────────────┐  BLE via  ┌───────────────────────────────┐
 │ Favorites menu (5 slots,    │  Garmin   │ ConnectIQ SDK listener        │
 │ Name+Number baked into      │  Connect  │ (foreground service)          │
 │ the widget at build time)   │  Mobile   │        │                      │
 │        │                    │ ────────► │        ▼                     │
 │        ▼                    │           │ TelecomManager.placeCall()    │
 │ Communications.transmit()   │           │ (CALL_PHONE permission)       │
 └─────────────────────────────┘           └───────────────────────────────┘
```

- **watch-app/** — Connect IQ widget (Monkey C). Shows up to 5 favorites, transmits `{"n": "<number>"}` on selection.
- **android-app/** — Android companion app (Kotlin). Receives the message via the Connect IQ Mobile SDK and places the call directly, no confirmation dialog.
- **releases/** — prebuilt binaries with placeholder (empty) favorites, ready to sideload. See [Quick install](#quick-install-prebuilt-binaries).

Personal use only — both apps are sideloaded. Neither is submitted to the Connect IQ Store or Google Play.

## Quick install (prebuilt binaries)

If you don't want to set up the Connect IQ SDK or Android build tooling, `releases/` has ready-to-install builds:

- `releases/GarminCallDialer-watch.prg` — the watch widget, with all 5 favorite slots empty. You'll fill them in yourself (see [Configuring your favorites](#configuring-your-favorites) below) — that requires the SDK, so this shortcut mainly saves you from setting up the Android toolchain.
- `releases/GarminCallDialer-android.apk` — the Android companion app, debug-signed. No personal data baked in.

Skip to [Installing on your devices](#installing-on-your-devices) to sideload these.

## Prerequisites (building from source)

- Garmin Connect Mobile installed on the phone, with the Instinct 2 Solar already paired.
- **Watch side**: [Connect IQ SDK Manager](https://developer.garmin.com/connect-iq/sdk/) + the "Monkey C" VS Code extension, with the **Instinct 2** device downloaded in the SDK Manager (Garmin uses one device profile, `instinct2`, for the whole Instinct 2 family including Solar — there's no separate "instinct2solar" profile).
- **Android side**: Android Studio (or just the command-line SDK) with `ANDROID_HOME` set, and a JDK 17 (`JAVA_HOME` — the Android Studio bundled JBR works: `.../Android Studio/jbr`).
- A Connect IQ developer signing key at `keys/developer_key.der` (`keys/developer_key.pem` for the private key). Generate one via the Monkey C extension ("Garmin: Generate Developer Key") if `keys/` is empty — it's gitignored and never committed.

## Building the watch app

```bash
cd watch-app
monkeyc -f monkey.jungle -y ../keys/developer_key.der -d instinct2 -o bin/DialerWidget.prg -w -r
```

`-r` produces a release build (no debug symbols) — use this for anything going on a real watch or into `releases/`. Drop `-r` for simulator/debug builds.

- **Simulator**: run the Connect IQ simulator (`connectiq`), then `monkeydo bin/DialerWidget.prg instinct2`.
- **Real watch**: see [Installing on your devices](#installing-on-your-devices).

> **Simulator gotcha**: the simulator persists Application Properties to disk across reloads just like a real device. If you change a default in `resources/properties`, a previous run's stored value silently wins until you edit it through the simulator's Settings menu (or erase the simulator's stored data). The same thing happens on a real watch — see [Configuring your favorites](#configuring-your-favorites).

## Building the Android app

```bash
cd android-app
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME="$HOME/AppData/Local/Android/Sdk" ./gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Installing on your devices

### Android companion app

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

(Or copy the APK to the phone and tap it in a file manager — Android will prompt to install from an unknown source.)

On first launch, tap through all three prompts — the app has no other UI and won't work until all three are done:

1. **Grant phone call permission** (`CALL_PHONE`) — required to place calls at all.
2. **Grant Bluetooth permission** (`BLUETOOTH_CONNECT`) — required on Android 12+; without it, the app crashes on startup (the OS refuses to start a `connectedDevice`-type foreground service without this permission actually being *granted*, not just declared in the manifest).
3. **Allow to run in background** (ignore battery optimization) — without this, Android will eventually kill the background listener while the phone is locked. This is the single biggest real-world reliability risk.

Once granted, it silently starts a foreground service (visible as a low-priority "Watching for watch..." notification) that listens for messages from the watch app and places calls via `TelecomManager`.

### Watch widget

The watch shows up as a plain USB mass-storage drive when connected — no special driver or "USB debugging" needed, just an unlocked/awake watch.

1. Connect the watch to your PC via USB. It may take 10–20 seconds to switch from its default USB mode to mass storage.
2. It mounts as a drive (Windows may not show a drive letter in Explorer immediately — check `Get-Disk` / `Get-Partition` in PowerShell if `E:` doesn't appear on its own).
3. Copy the `.prg` file into `GARMIN/APPS/` on that drive (any filename works, e.g. `GarminCallDialer-watch.prg`).
4. Safely eject the drive, then disconnect.
5. On the watch, press **up/down from the watch face** to scroll through widgets/glances — "Call Dialer" should now appear.

## Configuring your favorites

**Garmin Connect Mobile's Application Settings screen does not reliably work for sideloaded widgets** — we tried extensively (Activities & Apps, Appearance → Glances, Connect IQ Store → My Widgets) and none of them expose a settings/gear icon for an app that wasn't installed through the Connect IQ Store. "My Widgets" specifically only tracks store-installed apps.

So for now, favorites are configured **at build time**, directly in the source:

1. Edit `watch-app/resources/properties/properties.xml` — fill in `Favorite1Name`/`Favorite1Number` through `Favorite5Name`/`Favorite5Number`. Only slots with both a name and a number show up in the widget's menu.
2. Rebuild: `monkeyc -f monkey.jungle -y ../keys/developer_key.der -d instinct2 -o bin/DialerWidget.prg -w -r` (from `watch-app/`).
3. Copy the new `.prg` to the watch as described above.

**If your changes don't show up after reinstalling**: Connect IQ persists Application Properties to a file on the watch itself, separate from the `.prg`. If the widget was ever opened before with different property values, those old values win over your new defaults — the exact same "stale settings" behavior as the simulator. Fix: with the watch connected via USB, delete `GARMIN/APPS/SETTINGS/<yourfilename>.SET` (matching whatever you named the `.prg`), eject, then open the widget again on the watch — it'll re-read the fresh defaults compiled into the `.prg`.

Don't commit real favorite data (names/numbers) if this repo might ever go public or be shared — `properties.xml` is **not** gitignored, and the compiled `.prg` bakes the values in too (verify with `grep -a` on the binary before distributing it).

## Testing the end-to-end flow

1. Confirm Garmin Connect Mobile shows the watch as connected.
2. Open the companion app on the phone once so its foreground service is running (check the notification shade for "Watching for watch...").
3. On the watch, open the Call Dialer widget and select a favorite.
4. Watch shows "Calling `<name>`...", then automatically returns to the menu once BLE delivery is acknowledged — this only confirms the message reached the phone, not that the call connected (see [Known limitations](#known-limitations-v1)).
5. Phone should immediately start a call to that number — no confirmation prompt.

### Troubleshooting

- **Widget shows "No favorites"**: none of the 5 slots have both Name and Number set, or you're hitting the stale-settings gotcha above.
- **Android app crashes on launch after granting call permission**: check you're also granting the Bluetooth permission button — a `connectedDevice` foreground service on Android 14 needs `BLUETOOTH_CONNECT` actually granted, not just declared.
- **"Allow to run in background" button does nothing**: the manifest needs `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` declared, or Android silently no-ops the request without showing any dialog.
- **Notification says "Garmin Connect Mobile not running"**: launch Garmin Connect Mobile on the phone first — the ConnectIQ SDK proxies through it.
- **Notification says "No device"**: the watch isn't in the phone's list of known Connect IQ devices yet — open Garmin Connect Mobile and let it finish syncing with the watch at least once.
- **Watch shows "Calling..." and returns to the menu, but the phone never rings**: check `adb logcat -s DialerConnectionService:V CallTrigger:V AndroidRuntime:E` while triggering from the watch.
  - If you see `Message received from watch` and `TelecomManager.placeCall invoked` with no error, the request reached Android fine — the issue is downstream (SIM/carrier, Do Not Disturb, etc.).
  - If you see `ActivityTaskManager: Background activity launch blocked!` in the wider log, you're on an older build that used `startActivity(ACTION_CALL)` — Android silently blocks activity starts from a background service. `CallTrigger` now uses `TelecomManager.placeCall()` instead, which routes through the system Telecom service and isn't subject to that restriction.
- **Watch/phone doesn't show up in Windows after plugging in via USB**: give it 10–20 seconds — both devices switch USB modes after connecting and won't be visible immediately. If a device shows as "unauthorized" in `adb devices`, check the phone screen for a USB-debugging trust prompt; if none appears, `adb kill-server && adb start-server` often reissues it.

## Known limitations (v1)

- No call state feedback to the watch — the "Calling..." message only confirms BLE delivery, not that the call connected.
- Favorites are configured at build time (see [Configuring your favorites](#configuring-your-favorites)), not through Garmin Connect Mobile — sideloaded apps don't get an Application Settings entry point in the versions of Garmin Connect we tested against.
- Phone number validation on the Android side is a permissive regex (digits, spaces, `+`, `-`, `*`, `#`, parens, 3–20 chars) — permissive enough for gate/intercom codes, not a full E.164 validator.
- Up to 5 favorites, fixed slots (no add/remove, just fill in the ones you want).

## License

[Unlicense](LICENSE) — public domain. Do whatever you want with it.
