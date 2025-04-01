package top.kagg886.pmf.backend.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import top.kagg886.pmf.util.absolutePath

actual fun getDataBaseBuilder(): RoomDatabase.Builder<AppDatabase> = Room.databaseBuilder<AppDatabase>(
    name = databasePath.absolutePath().toString(),
).setDriver(BundledSQLiteDriver())
