package com.caa.app.di

import com.caa.app.data.db.DatabaseDriverFactory
import com.caa.app.data.repository.PictogramRepositoryImpl
import com.caa.app.data.repository.SettingsRepositoryImpl
import com.caa.app.data.settings.SettingsDataSource
import com.caa.app.db.CaaDatabase
import com.caa.app.domain.repository.PictogramRepository
import com.caa.app.domain.repository.SettingsRepository
import com.caa.app.platform.tts.SpeechEngine
import com.caa.app.platform.tts.SpeechEngineFactory
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule: Module = module {
    single<CaaDatabase> { CaaDatabase(get<DatabaseDriverFactory>().create()) }
    single<PictogramRepository> { PictogramRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get<SettingsDataSource>()) }
    single<SpeechEngine> { get<SpeechEngineFactory>().create() }
}

expect val platformModule: Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(commonModule, platformModule)
}
