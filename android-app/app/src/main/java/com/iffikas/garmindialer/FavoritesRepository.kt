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

    /**
     * Whether [number] is one of the saved favorites. Compared on digits and
     * a leading +, so "+372 600 0101" and "+3726000101" count as the same.
     */
    fun containsNumber(context: Context, number: String): Boolean {
        val wanted = normalize(number)
        if (wanted.isEmpty()) {
            return false
        }
        return load(context).any { normalize(it["number"] ?: "") == wanted }
    }

    private fun normalize(number: String): String {
        val trimmed = number.trim()
        val digits = trimmed.filter { it.isDigit() }
        return if (trimmed.startsWith("+")) "+$digits" else digits
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

    fun loadShowNumber(context: Context): Boolean {
        val prefs = context.getSharedPreferences(Constants.FAVORITES_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.SHOW_NUMBER_KEY, true)
    }

    fun saveShowNumber(context: Context, showNumber: Boolean) {
        val prefs = context.getSharedPreferences(Constants.FAVORITES_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.SHOW_NUMBER_KEY, showNumber).apply()
    }

    /**
     * The message sent to the watch apps, by both the "Send to watch" push
     * and the answer to the watch's "get_favorites" request, so the two can
     * never drift apart. "showNumber" tells the watch whether to display the
     * numbers next to the names.
     */
    fun buildWatchPayload(context: Context, favorites: List<Map<String, String>>): Map<String, Any> =
        mapOf("favorites" to favorites, "showNumber" to loadShowNumber(context))
}
