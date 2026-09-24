# Google Play store listing — draft

Use this as a starting point in Play Console. Edit freely before publishing.

## App name
Garmin Call Dialer

## Short description (max 80 characters)
Trigger a phone call from your Garmin watch — no phone in hand needed.

## Full description (max 4000 characters)

Garmin Call Dialer is a small companion app that lets a paired Garmin watch
place a phone call directly — one press from your wrist, no need to dig your
phone out of your pocket.

Originally built to solve one specific problem: dialing a building's gate or
intercom code without fumbling for a phone. It works for any short list of
favorites you call often.

How it works:
• Install this companion app on your phone and the matching Connect IQ widget
  on your Garmin watch.
• Open the companion app once to grant the required permissions and start the
  background listener.
• In the app, set up to 5 favorites — pick from your Contacts or type a name
  and number by hand (handy for gate codes and other non-contact numbers).
  Tap "Send to watch" to push them to your watch.
• On your watch, open the Call Dialer widget and select a favorite — your
  phone places the call immediately.

Permissions this app needs and why:
• Phone (CALL_PHONE) — to place the call when triggered from your watch.
• Bluetooth — to stay connected to your watch via the Garmin Connect Mobile
  SDK.
• Notifications — to show the required "Watching for watch..." background
  service notification.
• Battery optimization exemption (optional but recommended) — without it,
  Android may kill the background listener while your phone is locked.

Privacy: this app doesn't collect, transmit, or sell any of your data. All
communication happens directly between your phone and your own paired watch.
See the full privacy policy: <PRIVACY_POLICY_URL>

Requires: a compatible Garmin watch with the Garmin Call Dialer Connect IQ
widget installed, and Garmin Connect Mobile.

## Category
Tools

## Contact email
iffikas@gmail.com

## Privacy policy URL
https://iffikas.github.io/GarminCallDialer/privacy.html
(once GitHub Pages is enabled for this repo — see Publishing notes below)

## Graphic assets checklist
- [x] App icon, 512×512 PNG — `store-assets/play_store_icon_512.png` (simple blue-gray "GD" badge)
- [ ] Feature graphic, 1024×500 PNG or JPG — not yet created
- [ ] At least 2 phone screenshots (min 320px, max 3840px on the long edge) — not yet created

## Data safety form (Play Console) — talking points
- No data collected.
- No data shared with third parties.
- Data is not processed ephemerally by a server — there is no server; all
  data stays on-device.
- Permissions requested: Phone (call placement, no confirmation dialog by
  design), Bluetooth (device connection), Notifications (foreground service).

## Permissions declaration (CALL_PHONE / "Core Functionality" exemption)
When Play Console flags CALL_PHONE as a sensitive permission requiring
justification, the core answer is::
"The app's sole purpose is to let a paired Garmin smartwatch trigger a phone
call on the user's own phone with a single button press, replacing the need
to take the phone out to dial. Placing the call directly (via
TelecomManager.placeCall) without an extra confirmation step is the
requested behavior, not an incidental one — it mirrors pressing a speed-dial
button. The number originates only from a favorite the user has explicitly
configured themselves (picked from their own Contacts or typed by hand), never
from a remote source."

Be ready to attach: a short screen-recording of the watch → phone call flow
(Play sometimes requests a demo video for CALL_PHONE apps).

## Publishing notes

1. Google Play requires the privacy policy to be reachable at a public URL,
   not just a file in this repo. `docs/privacy.html` (an HTML copy of
   `PRIVACY.md`) is ready to serve via GitHub Pages:
   - Push this repo to GitHub (must be a public repo, or a private one on a
     paid plan — Pages doesn't serve from private repos on the free tier).
   - In the repo, go to **Settings → Pages** → under "Build and deployment",
     set **Source: Deploy from a branch**, **Branch: master**, **Folder: /docs**, then Save.
   - After a minute or two it'll be live at
     `https://iffikas.github.io/GarminCallDialer/privacy.html`.
2. This is a niche personal-use tool with a small, fixed feature set (5
   favorites, one Garmin watch line). Expect Play's review for CALL_PHONE
   apps to take a few days and possibly a round of clarifying questions —
   answer honestly using the talking points above rather than removing the
   direct-dial behavior, since that's the whole point of the app.
3. `versionCode`/`versionName` in `android-app/app/build.gradle.kts` need to
   be bumped for every new release you upload to Play Console.
