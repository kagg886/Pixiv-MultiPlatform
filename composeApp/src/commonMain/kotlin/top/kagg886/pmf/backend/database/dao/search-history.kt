package top.kagg886.pmf.backend.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.backend.database.converters.HistoryConverter


@Dao
interface SearchHistoryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistory)


    @Query("SELECT * FROM SearchHistory ORDER BY createTime DESC")
    fun allFlow(): Flow<List<SearchHistory>>

    @Delete
    suspend fun delete(item: SearchHistory)

    @Query("DELETE FROM SearchHistory")
    suspend fun clear()
}

@Entity
@TypeConverters(HistoryConverter::class)
data class SearchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val initialSort: SearchSort,
    val initialTarget: SearchTarget,
    val keyword: List<String> = listOf(),
    val createTime: Long = Clock.System.now().toEpochMilliseconds(),
)
