package top.kagg886.pmf.backend.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.backend.dataPath
import top.kagg886.pmf.backend.database.dao.*
import top.kagg886.pmf.util.absolutePath

@Database(
    entities = [IllustHistory::class, NovelHistory::class, DownloadItem::class, SearchHistory::class],
    version = BuildConfig.DATABASE_VERSION,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun illustHistoryDAO(): IllustHistoryDAO
    abstract fun novelHistoryDAO(): NovelHistoryDAO
    abstract fun downloadDAO(): DownloadDao
    abstract fun searchHistoryDAO(): SearchHistoryDAO
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

val databasePath = dataPath.resolve("app.db").absolutePath().toString()
expect fun dataBaseBuilder(): RoomDatabase.Builder<AppDatabase>
