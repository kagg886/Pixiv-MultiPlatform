package top.kagg886.pmf.backend.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.backend.database.converters.HistoryConverter
import top.kagg886.pmf.backend.database.converters.NovelConverter
import top.kagg886.pmf.ui.route.main.search.SearchTab


@Dao
interface SearchHistoryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistory)


    @Query("SELECT * FROM SearchHistory ORDER BY createTime DESC")
    fun allFlow(): Flow<List<SearchHistory>>

    @Delete
    suspend fun delete(item: SearchHistory)
}

@Entity
@TypeConverters(HistoryConverter::class)
data class SearchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val initialSort: SearchSort,
    val initialTarget: SearchTarget,
    val keyword: List<String> = listOf(),
    val createTime: Long = System.currentTimeMillis(),






    @Deprecated("v2版本使用数组存储")
    val initialKeyWords: String = "",
    @Deprecated("v2版本不使用此属性")
    val tab: SearchTab = SearchTab.ILLUST,
)