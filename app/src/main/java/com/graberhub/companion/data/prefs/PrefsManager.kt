package com.graberhub.companion.data.prefs

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.graberhub.companion.data.models.KidConfig
import com.graberhub.companion.data.models.ScheduleTemplate

class PrefsManager(context: Context) {

    private val prefs = context.getSharedPreferences("graber_hub_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        // The Pi received a new DHCP address. Migrate existing installs that
        // still have the old home-WiFi address saved.
        if (prefs.getString(KEY_SERVER_URL, null) == LEGACY_HOME_URL) {
            prefs.edit().putString(KEY_SERVER_URL, HOME_URL).apply()
        }
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_KIDS = "kids_config"
        private const val KEY_CUSTOM_SCHEDULE_TEMPLATES = "custom_schedule_templates"
        private const val KEY_PHOTO_SOURCE = "photo_source"

        private const val LEGACY_HOME_URL = "http://192.168.86.43/"
        const val HOME_URL = "http://192.168.86.24/"
        const val TAILSCALE_URL = "http://100.72.250.82/"
        const val DEFAULT_SERVER = HOME_URL

        val DEFAULT_KIDS = listOf(
            KidConfig(1, "Aurora", "#ec5281"),
            KidConfig(2, "Deandre", "#03a9f4"),
            KidConfig(3, "Alexia", "#a78bfa"),
            KidConfig(4, "Truett", "#4ade80")
        )
    }

    var serverUrl: String
        get() {
            val url = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER) ?: DEFAULT_SERVER
            // Always ensure trailing slash for Retrofit
            return if (url.endsWith("/")) url else "$url/"
        }
        set(value) {
            // Store with trailing slash to avoid issues
            val url = if (value.endsWith("/")) value else "$value/"
            prefs.edit().putString(KEY_SERVER_URL, url).apply()
        }

    val isHomeNetwork: Boolean
        get() = serverUrl == HOME_URL

    val isTailscaleNetwork: Boolean
        get() = serverUrl == TAILSCALE_URL

    val alternateUrl: String
        get() = if (isHomeNetwork) TAILSCALE_URL else HOME_URL

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

    var customScheduleTemplates: List<ScheduleTemplate>
        get() {
            val json = prefs.getString(KEY_CUSTOM_SCHEDULE_TEMPLATES, null) ?: return emptyList()
            return try {
                val type = object : TypeToken<List<ScheduleTemplate>>() {}.type
                gson.fromJson<List<ScheduleTemplate>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_SCHEDULE_TEMPLATES, gson.toJson(value)).apply()
        }

    // "uploaded" or "google"
    var photoSource: String
        get() = prefs.getString(KEY_PHOTO_SOURCE, "uploaded") ?: "uploaded"
        set(value) { prefs.edit().putString(KEY_PHOTO_SOURCE, value).apply() }
}
