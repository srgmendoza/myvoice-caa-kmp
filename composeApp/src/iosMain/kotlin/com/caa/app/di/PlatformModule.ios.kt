package com.caa.app.di

import com.caa.app.data.db.DatabaseDriverFactory
import com.caa.app.data.settings.IosSettingsDataSource
import com.caa.app.data.settings.SettingsDataSource
import com.caa.app.platform.tts.SpeechEngineFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory() }
    single { SpeechEngineFactory() }
    single<SettingsDataSource> { IosSettingsDataSource() }
}
