package com.caa.app.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.caa.app.db.CaaDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(
            schema = CaaDatabase.Schema,
            name = "caa.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true)
                )
            }
        )
}
