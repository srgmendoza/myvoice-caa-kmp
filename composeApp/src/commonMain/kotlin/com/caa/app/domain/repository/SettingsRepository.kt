package com.caa.app.domain.repository

import com.caa.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun setDebounceMs(value: Long)
}
