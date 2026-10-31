package com.iffikas.garmindialer

object Constants {
    // Both watch apps this phone app serves. They are separate Connect IQ
    // apps with their own ids but the same message protocol:
    //  - the v1 widget (this repo's watch-app/manifest.xml), for older
    //    devices such as the Instinct 2
    //  - the v2 device app + glance (GarminCallDialer2), for API 4.0+
    //    devices such as the Venu 3
    val WATCH_APP_IDS = listOf(
        "4485b9b4-ced5-465d-a9c2-351e26def6ce",
        "90ad9ff5-1444-4ea4-a9bb-24b266ac7bda"
    )

    const val NOTIFICATION_CHANNEL_ID = "dialer_connection"
    const val NOTIFICATION_ID = 1

    // Matches the watch widget's fixed number of menu slots.
    const val MAX_FAVORITES = 5

    const val FAVORITES_PREFS_NAME = "favorites"
    fun favoriteNameKey(index: Int) = "favorite_${index}_name"
    fun favoriteNumberKey(index: Int) = "favorite_${index}_number"
}
