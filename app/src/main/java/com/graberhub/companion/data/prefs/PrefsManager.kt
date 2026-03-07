package com.graberhub.companion.data.prefs

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.graberhub.companion.data.models.KidConfig

class PrefsManager(context: Context) {

    companion object {
        private const val DEFAULT_SERVER = "http://192.168.86.43"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_KIDS = "kids_config"
        private val DEFAULT_KIDS = listOf(
            KidConfig(1, "Aurora", "#ec5281"),
            KidConfig(2, "Deandre", "#03a9f4"),
            KidConfig(3, "Alexia", "#a78bfa"),
            KidConfig(4, "Truett", "#4ade80")
        )
    }

    private val prefs = context.getSharedPreferences("graber_hub_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER) ?: DEFAULT_SERVER
        set(value) {
            prefs.edit().putString(KEY_SERVER_URL, value.trimEnd('/')).apply()
        }

    var kidsConfig: List<KidConfig>
        get() {
            val json = prefs.getString(KEY_KIDS, null) ?: return DEFAULT_KIDS
            return try {
                val type = object : TypeToken<List<KidConfig>>() {}.type
                gson.fromJson<List<KidConfig>>(json, type) ?: DEFAULT_KIDS
            } catch (e: Exception) {
                DEFAULT_KIDS
            }
        }
        set(value) {
            prefs.edit().putString(KEY_KIDS, gson.toJson(value)).apply()
        }

    fun resetKids() {
        prefs.edit().remove(KEY_KIDS).apply()
    }
}
