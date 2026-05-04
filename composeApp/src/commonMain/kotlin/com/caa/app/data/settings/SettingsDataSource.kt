package com.caa.app.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsDataSource {
    fun observeDebounceMs(): Flow<Long>
    suspend fun setDebounceMs(value: Long)
}
