package top.kagg886.pmf.ui.route.main.history

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.ui.util.IllustFetchViewModel

class HistoryIllustViewModel : IllustFetchViewModel(), KoinComponent {
    private val database by inject<AppDatabase>()

    // fixme(tarsin): support placeholders
    override fun source() = Pager(
        PagingConfig(30, enablePlaceholders = false),
        pagingSourceFactory = { database.illustHistoryDAO().source() },
    ).flow.map { data -> data.map { h -> h.illust } }
}
