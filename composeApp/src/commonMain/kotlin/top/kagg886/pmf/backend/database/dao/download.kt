package top.kagg886.pmf.backend.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
    suspend fun allSuspend():List<DownloadItem>
}

@Entity
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val url: String,
    val success: Boolean
)