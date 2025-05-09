package top.kagg886.pmf.backend.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pmf.backend.database.converters.NovelConverter

@Dao
interface NovelHistoryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: NovelHistory)

    @Query("SELECT * FROM NovelHistory ORDER BY createTime DESC")
    fun source(): PagingSource<Int, NovelHistory>
}

@Entity
@TypeConverters(NovelConverter::class)
data class NovelHistory(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val novel: Novel,
    val createTime: Long,
)
