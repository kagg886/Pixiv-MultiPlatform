package top.kagg886.pmf.backend.database

import androidx.room.Room
import androidx.room.RoomDatabase
import top.kagg886.pmf.PMFApplication
import top.kagg886.pmf.util.absolutePath

actual fun getDataBaseBuilder(): RoomDatabase.Builder<AppDatabase> = Room.databaseBuilder<AppDatabase>(
    name = databasePath.absolutePath().toString(),
    context = PMFApplication.getApp(),
)
