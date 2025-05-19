package top.kagg886.pmf.backend.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pmf.backend.database.converters.IllustConverter

@Dao
interface IllustHistoryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: IllustHistory)

    @Query("SELECT * FROM IllustHistory ORDER BY createTime DESC")
    fun source(): PagingSource<Int, IllustHistory>
}

@Entity
@TypeConverters(IllustConverter::class)
data class IllustHistory(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val illust: Illust,
    val createTime: Long,
)
