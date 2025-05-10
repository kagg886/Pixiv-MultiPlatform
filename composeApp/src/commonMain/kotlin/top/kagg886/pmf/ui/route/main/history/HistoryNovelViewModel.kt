package top.kagg886.pmf.ui.route.main.history

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.ui.util.NovelFetchViewModel

class HistoryNovelViewModel : NovelFetchViewModel(), KoinComponent {
    private val database by inject<AppDatabase>()
    override fun source() = Pager(
        PagingConfig(20, enablePlaceholders = false),
        pagingSourceFactory = { database.novelHistoryDAO().source() },
    ).flow.map { d -> d.map { h -> h.novel } }
}
