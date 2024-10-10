package top.kagg886.pmf.ui.route.main.history

import org.koin.core.component.inject
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.NovelResult
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import kotlin.coroutines.CoroutineContext

class HistoryNovelViewModel : NovelFetchViewModel() {

    private val database by inject<AppDatabase>()
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Novel> {
        return object : InfinityRepository<Novel>(coroutineContext) {
            private var page: Int = 1
            override suspend fun onFetchList(): List<Novel> {
                return database.novelHistoryDAO().getByPage(page).map { it.novel }.apply {
                    page++
                }
            }
        }
    }

}