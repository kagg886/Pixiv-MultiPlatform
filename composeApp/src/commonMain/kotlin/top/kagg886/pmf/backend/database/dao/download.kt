package top.kagg886.pmf.backend.database.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pmf.backend.database.converters.IllustConverter

@Dao
interface DownloadDao {
    @Insert
    suspend fun insert(item: DownloadItem): Long

    @Query("SELECT * FROM DownloadItem WHERE id = :id")
    suspend fun find(id: Long): DownloadItem?

    @Update
    suspend fun update(item: DownloadItem)

    @Query("SELECT * FROM DownloadItem order by id desc")
    fun all(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM DownloadItem")
    suspend fun allSuspend(): List<DownloadItem>
}

@Entity
@TypeConverters(IllustConverter::class)
data class DownloadItem(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val illust: Illust,
    val success: Boolean,
    val progress: Float = -1f,
    val createTime: Long = Clock.System.now().toEpochMilliseconds(),
)
