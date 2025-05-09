package top.kagg886.pmf.backend.database

import androidx.room.Room
import androidx.sqlite.driver.NativeSQLiteDriver

actual fun databaseBuilder() = Room.databaseBuilder<AppDatabase>(name = databasePath).setDriver(NativeSQLiteDriver())
