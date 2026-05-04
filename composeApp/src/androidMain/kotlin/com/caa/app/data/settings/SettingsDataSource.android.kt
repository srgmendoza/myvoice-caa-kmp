package com.caa.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.caa.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "caa_settings")

class AndroidSettingsDataSource(private val context: Context) : SettingsDataSource {
    private val keyDebounce = longPreferencesKey("debounce_ms")

    override fun observeDebounceMs(): Flow<Long> =
        context.dataStore.data.map { it[keyDebounce] ?: AppSettings().debounceMs }

    override suspend fun setDebounceMs(value: Long) {
        context.dataStore.edit { it[keyDebounce] = value }
    }
}
