package com.neverexpire.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(val alertDays: Int = 3, val notifyHour: Int = 9)

class SettingsRepository private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("neverexpire_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(
        AppSettings(prefs.getInt("alertDays", 3), prefs.getInt("notifyHour", 9))
    )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(s: AppSettings) {
        prefs.edit().putInt("alertDays", s.alertDays).putInt("notifyHour", s.notifyHour).apply()
        _settings.value = s
    }

    companion object {
        @Volatile private var INSTANCE: SettingsRepository? = null
        fun get(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) { INSTANCE ?: SettingsRepository(context).also { INSTANCE = it } }
    }
}
