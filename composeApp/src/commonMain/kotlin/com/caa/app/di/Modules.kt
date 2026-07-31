package com.caa.app.di

import com.caa.app.data.db.DatabaseDriverFactory
import com.caa.app.data.remote.ArasaacClient
import com.caa.app.data.remote.ArasaacFileSystem
import com.caa.app.data.remote.ArasaacImageDownloader
import com.caa.app.data.repository.ArasaacRepository
import com.caa.app.data.repository.PictogramRepositoryImpl
import com.caa.app.data.repository.SettingsRepositoryImpl
import com.caa.app.data.settings.SettingsDataSource
import com.caa.app.db.CaaDatabase
import com.caa.app.domain.repository.PictogramRepository
import com.caa.app.domain.repository.SettingsRepository
import com.caa.app.platform.tts.SpeechEngine
import com.caa.app.platform.tts.SpeechEngineFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule: Module = module {
    single<CaaDatabase> { CaaDatabase(get<DatabaseDriverFactory>().create()) }
    single<PictogramRepository> { PictogramRepositoryImpl(get(), get<ArasaacFileSystem>()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get<SettingsDataSource>()) }
    single<SpeechEngine> { get<SpeechEngineFactory>().create() }

    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
            }
        }
    }
    single { ArasaacClient(get()) }
    single { ArasaacImageDownloader(get(), get<ArasaacFileSystem>()) }
    single { ArasaacRepository(get(), get(), get()) }
}

expect val platformModule: Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(commonModule, platformModule)
}
