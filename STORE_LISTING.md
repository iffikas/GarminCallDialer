# Store listings — ready to paste

Two stores, two listings:

1. **Google Play**: the Android companion app (`com.iffikas.garmindialer`, v1.2.1 / versionCode 7)
2. **Garmin Connect IQ Store**: the watch widget ("Wrist Dialer (widget)" on the watch, 41 devices including the Instinct 2 series, API 3.2+)

Upload files:

| Store | File |
|---|---|
| Play | `android-app/app/build/outputs/bundle/release/app-release.aab` (build with `./gradlew bundleRelease`) |
| Connect IQ | `watch-app/bin/GarminDialer-widget.iq` (build with `monkeyc -f monkey.jungle -y ../keys/developer_key.der -e -o bin/GarminDialer-widget.iq -w -r`) |

Donation link used below: https://ko-fi.com/A5N029BZWH. Store descriptions are plain text, so the Ko-fi button script can't run there. The button itself is on the GitHub Pages site (`docs/index.html`).

---

## 1. Google Play

### App name (max 30)
Wrist Dialer for Garmin

### Short description (max 80)
Call your favorite numbers from your Garmin watch without taking out your phone

### Full description (max 4000)

Wrist Dialer lets your Garmin watch start a phone call on your Android phone with a single press, so you don't need to take your phone out of your pocket.

It was built for one everyday problem: calling a gate or intercom number while your hands are full. It works for any short list of numbers you call often: home, family, the office gate, a taxi.

HOW IT WORKS
• Install this app on your phone and the free "Wrist Dialer for Garmin (widget)" from the Connect IQ Store on your watch.
• Open this app once to grant permissions. A small background service then listens for your watch.
• Add up to 5 favorites. Pick them from Contacts or type a name and number by hand, which is handy for gate codes that aren't saved as contacts. Tap "Send to watch".
• On your watch, open the widget and select a favorite. Your phone places the call straight away.

The watch also asks your phone for the latest list each time you open the widget, so it stays in sync even if you forget to send changes.

PERMISSIONS AND WHY
• Phone: places the call when your watch asks for it.
• Nearby devices / Bluetooth: keeps the connection to your watch through Garmin Connect.
• Notifications: shows the required "Watching for call requests" notification for the background service.
• Battery optimization exemption (optional, recommended): stops Android from killing the listener while your phone is locked.

PRIVACY AND SECURITY
• No account, ads, analytics or tracking.
• Your favorites are stored only on your phone and your watch. They are not uploaded anywhere, and the app opts out of Google cloud backup.
• Only call requests relayed by Garmin Connect are accepted. Other apps on your phone cannot use this app to place calls.
• Service codes (numbers starting with * or #) are always refused, so a call request can never change your phone's call-forwarding or other network settings.

REQUIREMENTS
• Android 8.0 or newer
• Garmin Connect app, paired with your watch
• A compatible Garmin watch with "Wrist Dialer for Garmin (widget)" installed from the Connect IQ Store

SUPPORT THE APP
Wrist Dialer is free, with no ads. If it's useful to you, you can support development at https://ko-fi.com/A5N029BZWH

Wrist Dialer is an independent app. It is not made or endorsed by Garmin Ltd.

### What's new (v1.2.1, max 500)
New name: Wrist Dialer for Garmin.
Security and privacy update:
• Call requests are accepted only when relayed by Garmin Connect, so other apps cannot trigger calls.
• Service codes (* and # numbers) are now refused.
• Favorites are no longer included in Google cloud backup.
• Phone numbers are no longer written to the system log.

### Category / contact
- Category: **Tools**
- Contact email: iffikas@gmail.com
- Website: https://iffikas.github.io/GarminCallDialer/
- Privacy policy URL: https://iffikas.github.io/GarminCallDialer/privacy.html

### Graphic assets (`store-assets/`)
- App icon 512×512: `play_store_icon_512.png`
- Feature graphic 1024×500: `feature_graphic_1024x500.png`
- Phone screenshot 1: `play_screenshot_1_main_1440x2880.png` (main screen)
- Phone screenshot 2: `play_screenshot_2_favorites_1440x2880.png` (favorites editor, sample numbers)

### Data safety form
- Does your app collect or share any of the required user data types? **No**
- Data encrypted in transit / deletion requests: not applicable (no data leaves the device through this app; uninstalling removes all data)

### Foreground service declaration (Play Console → App content)
- Type: **Connected device** (`FOREGROUND_SERVICE_CONNECTED_DEVICE`)
- Description: "The app keeps a foreground service running so that it can receive call requests from the user's paired Garmin smartwatch over Bluetooth (via the Garmin Connect app) at any time, including while the phone is locked. If the service is stopped, pressing a favorite on the watch no longer places a call. The service shows a persistent notification and does no other work."
- Video: a short screen recording (unlisted YouTube link) showing: open the app → notification visible → select a favorite on the watch → phone starts the call.

### CALL_PHONE justification (if Play asks)
"The app's only purpose is to let the user's paired Garmin smartwatch start a phone call on the user's own phone with one button press. Placing the call directly via TelecomManager.placeCall, without an extra confirmation, is the core requested behavior, just like a speed-dial button. Numbers come only from favorites the user configured (picked from their Contacts or typed by hand). Only messages relayed by the Garmin Connect app (verified by its signing certificate) are accepted, and MMI/USSD service codes are always rejected."

### Battery optimization (if Play asks about REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
"Companion app for a Bluetooth smartwatch. It must keep receiving call requests from the watch while the phone is in Doze; otherwise a button press on the watch is silently lost. The exemption is optional and only requested when the user taps 'Allow to run in background'."

---

## 2. Garmin Connect IQ Store (watch widget)

### App name
Wrist Dialer for Garmin (widget)

### App type / category
Widget. Category: **Tools** (or "Productivity", if Tools isn't offered)

### Description

Call your favorite numbers straight from your wrist.

Open the widget, select a favorite, and your paired Android phone starts the call at once. You don't need to take out your phone. It's ideal for gate and intercom numbers, family, or anyone you call often.

REQUIRES THE FREE ANDROID COMPANION APP
Install "Wrist Dialer for Garmin" from Google Play. Your phone places the call because the watch cannot do it on its own. In the companion app you set up to 5 favorites (from Contacts or typed by hand) and send them to the watch. The widget also fetches the latest list automatically each time you open it.

HOW TO USE
1. Install the companion app on your Android phone and grant its permissions.
2. Add your favorites and tap "Send to watch".
3. On the watch, open the Wrist Dialer widget, scroll to a favorite and press START/SELECT.
4. The watch shows "Call being made on phone", and your phone dials.

NOTES
• Android only. iPhone is not supported.
• Your phone must be connected to the watch through Garmin Connect.
• No data leaves your own phone and watch.

SUPPORT THE APP
Free, with no ads. If it's useful to you, you can support development at https://ko-fi.com/A5N029BZWH

Wrist Dialer for Garmin is an independent app and is not made or endorsed by Garmin Ltd.

### What's new (v1.2.1)
• New name: Wrist Dialer for Garmin.
• The watch now accepts at most 5 favorites from the phone and checks every entry before saving it.

### Assets
- Icon: built from `watch-app/resources*/drawables` (already in the `.iq`)
- Hero image 1440×720: `store-assets/hero_image_1440x720.png`
- Screenshots (simulator, sample numbers), in `store-assets/`:
  - `connectiq_instinct2_1_menu.png`, `connectiq_instinct2_2_calling.png`, `connectiq_instinct2_3_call_placed.png`
  - `connectiq_fenix6pro_1_menu.png`, `connectiq_fenix6pro_2_calling.png`, `connectiq_fenix6pro_3_call_placed.png`

### Retaking watch screenshots (simulator)
1. Temporarily put sample names and numbers into `watch-app/resources/properties/properties.xml` (don't commit them), then build: `monkeyc -f monkey.jungle -y ../keys/developer_key.der -d instinct2 -o bin/DialerWidget.prg -w`
2. Start the simulator (`connectiq`), then run `monkeydo bin/DialerWidget.prg instinct2`.
3. Click **OK** on the "There is no data connection" dialog (the widget asks the phone for favorites on start).
4. Use *File → Save Screen Capture* for: the favorites menu, "Calling Home...", and "Call being made on phone" (select a favorite).
5. Undo the `properties.xml` change: `git checkout -- watch-app/resources/properties/properties.xml`

---

## Publishing notes

1. The privacy policy must be at a public URL. `docs/` is served by GitHub Pages (Settings → Pages → branch `master`, folder `/docs`).
2. Bump `versionCode`/`versionName` in `android-app/app/build.gradle.kts` for every Play upload.
3. Play's review of apps that place calls can take a few days. Use the justifications above. Don't remove the direct-dial behavior, because it's the whole point of the app.
