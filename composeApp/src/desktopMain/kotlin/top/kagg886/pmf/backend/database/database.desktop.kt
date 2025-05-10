package top.kagg886.pmf.backend.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun databaseBuilder() = Room.databaseBuilder<AppDatabase>(name = databasePath).setDriver(BundledSQLiteDriver())
