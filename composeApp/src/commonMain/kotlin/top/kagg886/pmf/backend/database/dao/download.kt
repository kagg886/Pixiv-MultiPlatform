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
import kotlinx.serialization.json.Json
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.novel.Novel
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

enum class DownloadItemType {
    ILLUST,
    NOVEL,
}

@Entity
@TypeConverters(IllustConverter::class)
data class DownloadItem(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val title: String,
    val meta: DownloadItemType,
    val data: String,
    val success: Boolean,
    val progress: Float = -1f,
    val createTime: Long = Clock.System.now().toEpochMilliseconds(),
)

fun DownloadItem(
    id: Long,
    illust: Illust,
    success: Boolean,
    progress: Float = -1f,
    createTime: Long = Clock.System.now().toEpochMilliseconds(),
) = DownloadItem(id, illust.title, DownloadItemType.ILLUST, Json.encodeToString(illust), success, progress, createTime)

fun DownloadItem(
    id: Long,
    novel: Novel,
    success: Boolean,
    progress: Float = -1f,
    createTime: Long = Clock.System.now().toEpochMilliseconds(),
) = DownloadItem(id, novel.title, DownloadItemType.NOVEL, Json.encodeToString(novel), success, progress, createTime)

val DownloadItem.illust: Illust
    get() {
        check(meta == DownloadItemType.ILLUST) { "Not an illust" }
        return Json.decodeFromString<Illust>(data)
    }
val DownloadItem.novel: Novel
    get() {
        check(meta == DownloadItemType.NOVEL) { "Not a novel" }
        return Json.decodeFromString<Novel>(data)
    }
