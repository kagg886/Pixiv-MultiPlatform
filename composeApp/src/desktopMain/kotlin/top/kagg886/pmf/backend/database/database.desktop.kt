package top.kagg886.pmf.backend.database

import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getDataBaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
        name = databasePath.absolutePath,
    )
}