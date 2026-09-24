# Privacy Policy — Garmin Call Dialer

_Last updated: 2026-09-24_

Garmin Call Dialer ("the app") is a small companion app that lets a paired Garmin
watch trigger a phone call on your Android phone.

## What the app does

- Receives a phone number from the Connect IQ widget on your Garmin watch (over
  Bluetooth, via the Garmin Connect Mobile / Connect IQ Mobile SDK) and places a
  call to that number using Android's TelecomManager.
- Lets you define up to 5 favorites (a name and a number each) either by picking
  a contact from your phone's Contacts app or by typing a name/number manually,
  and sends that list to your watch over the same Bluetooth connection.

## Data collection

The app does not collect, store, transmit, or sell any data to the developer or
to any third party. Specifically:

- **No analytics, tracking, or advertising SDKs** are included in the app.
- **No servers**: all communication happens directly between your phone and your
  own paired Garmin watch over Bluetooth (via the Garmin Connect Mobile app).
  Nothing is sent to the developer or to any cloud service operated by the
  developer.
- **Contacts**: when you use "Pick contact," Android's own contact picker returns
  only the single name and number you selected. The app does not request
  general access to your address book, does not read your full contact list,
  and does not store contact data anywhere except locally on your own phone
  and watch (see below).
- **Favorites data** (names and numbers you configure) is stored only:
  - locally on your phone, in the app's private storage, and
  - locally on your paired watch, in the watch's on-device storage.
  This data never leaves your own devices.

## Permissions used

| Permission | Why it's needed |
|---|---|
| `CALL_PHONE` | To place the call directly when triggered from the watch, without showing a confirmation dialog. |
| `BLUETOOTH_CONNECT` | Required by Android to run the background service that listens for messages from the Garmin Connect Mobile SDK. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Keeps the Bluetooth listener alive in the background so a watch tap can trigger a call even when the phone is locked. |
| `POST_NOTIFICATIONS` | Shows the required persistent notification for the foreground service. |
| `RECEIVE_BOOT_COMPLETED` | Restarts the listening service after the phone reboots. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Lets you exempt the app from battery optimization so the background listener isn't killed while the phone is locked. |

## Data deletion

Since no data is collected by the developer, there is nothing to request
deletion of. Uninstalling the app removes all locally stored favorites from
your phone; clearing the watch app's data (or reinstalling it) removes
favorites stored on the watch.

## Contact

Questions about this policy can be sent to: iffikas@gmail.com
