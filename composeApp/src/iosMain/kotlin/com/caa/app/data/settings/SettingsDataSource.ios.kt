package com.caa.app.data.settings

import com.caa.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

class IosSettingsDataSource : SettingsDataSource {
    private val key = "debounce_ms"
    private val defaults = NSUserDefaults.standardUserDefaults
    private val state = MutableStateFlow(loadInitial())

    private fun loadInitial(): Long {
        val stored = defaults.objectForKey(key) as? Number
        return stored?.toLong() ?: AppSettings().debounceMs
    }

    override fun observeDebounceMs(): Flow<Long> = state

    override suspend fun setDebounceMs(value: Long) {
        defaults.setInteger(value, key)
        state.value = value
    }
}
