package top.kagg886.pmf.backend.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import top.kagg886.pmf.backend.database.dao.*

@Database(
    entities = [IllustHistory::class, NovelHistory::class, DownloadItem::class],
    version = 1
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun illustHistoryDAO(): IllustHistoryDAO
    abstract fun novelHistoryDAO(): NovelHistoryDAO
    abstract fun downloadDAO(): DownloadDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

//@Dao
//interface TodoDao {
//    @Insert
//    suspend fun insert(item: TodoEntity)
//
//    @Query("SELECT count(*) FROM TodoEntity")
//    suspend fun count(): Int
//
//    @Query("SELECT * FROM TodoEntity")
//    fun getAllAsFlow(): Flow<List<TodoEntity>>
//}
//
//@Entity
//data class TodoEntity(
//    @PrimaryKey(autoGenerate = true) val id: Long = 0,
//    val title: String,
//    val content: String
//)