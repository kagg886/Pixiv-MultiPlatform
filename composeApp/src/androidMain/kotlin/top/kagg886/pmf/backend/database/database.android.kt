package top.kagg886.pmf.backend.database

import androidx.room.Room
import top.kagg886.pmf.PMFApplication

actual fun databaseBuilder() = Room.databaseBuilder<AppDatabase>(name = databasePath, context = PMFApplication.getApp())
