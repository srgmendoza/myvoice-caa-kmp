package com.caa.app.data.repository

import com.caa.app.data.settings.SettingsDataSource
import com.caa.app.domain.model.AppSettings
import com.caa.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val source: SettingsDataSource
) : SettingsRepository {
    override fun observe(): Flow<AppSettings> =
        source.observeDebounceMs().map { AppSettings(debounceMs = it) }

    override suspend fun setDebounceMs(value: Long) = source.setDebounceMs(value)
}
