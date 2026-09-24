package com.iffikas.garmindialer

import android.content.Context

/**
 * Persisted favorites, shared between FavoritesActivity (editing UI) and
 * DialerConnectionService (answers the watch's "get_favorites" requests so
 * it can self-sync on open instead of only receiving pushes).
 */
object FavoritesRepository {

    fun load(context: Context): List<Map<String, String>> {
        val prefs = context.getSharedPreferences(Constants.FAVORITES_PREFS_NAME, Context.MODE_PRIVATE)
        val favorites = mutableListOf<Map<String, String>>()
        for (i in 0 until Constants.MAX_FAVORITES) {
            val name = prefs.getString(Constants.favoriteNameKey(i), null)
            val number = prefs.getString(Constants.favoriteNumberKey(i), null)
            if (!name.isNullOrEmpty() && !number.isNullOrEmpty()) {
                favorites.add(mapOf("name" to name, "number" to number))
            }
        }
        return favorites
    }

    fun save(context: Context, favorites: List<Map<String, String>>) {
        val prefs = context.getSharedPreferences(Constants.FAVORITES_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            for (i in 0 until Constants.MAX_FAVORITES) {
                if (i < favorites.size) {
                    putString(Constants.favoriteNameKey(i), favorites[i]["name"])
                    putString(Constants.favoriteNumberKey(i), favorites[i]["number"])
                } else {
                    remove(Constants.favoriteNameKey(i))
                    remove(Constants.favoriteNumberKey(i))
                }
            }
        }.apply()
    }
}
