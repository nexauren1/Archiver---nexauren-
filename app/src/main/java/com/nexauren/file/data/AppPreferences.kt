package com.nexauren.file.data

import android.content.Context

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("nexauren_file", Context.MODE_PRIVATE)

    fun getRootUri(): String? = prefs.getString("root_uri", null)
    fun setRootUri(uri: String) = prefs.edit().putString("root_uri", uri).apply()

    fun getFavorites(): Set<String> =
        prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()

    fun setFavorites(value: Set<String>) =
        prefs.edit().putStringSet("favorites", value).apply()

    fun getRecents(): List<String> =
        prefs.getString("recents", "")?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

    fun addRecent(uri: String) {
        val updated = (listOf(uri) + getRecents()).distinct().take(50)
        prefs.edit().putString("recents", updated.joinToString("|")).apply()
    }
}
