package com.iffikas.garmindialer

object Constants {
    // Must match the id="..." attribute in watch-app/manifest.xml
    const val WATCH_APP_ID = "4485b9b4-ced5-465d-a9c2-351e26def6ce"

    const val NOTIFICATION_CHANNEL_ID = "dialer_connection"
    const val NOTIFICATION_ID = 1

    // Matches the watch widget's fixed number of menu slots.
    const val MAX_FAVORITES = 5

    const val FAVORITES_PREFS_NAME = "favorites"
    fun favoriteNameKey(index: Int) = "favorite_${index}_name"
    fun favoriteNumberKey(index: Int) = "favorite_${index}_number"
}
