package top.kagg886.pmf.backend.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
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
data class SearchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val initialSort:SearchSort,
    val initialTarget:SearchTarget,
    val initialKeyWords:String,
    val tab:SearchTab,
    val createTime: Long = System.currentTimeMillis()
)