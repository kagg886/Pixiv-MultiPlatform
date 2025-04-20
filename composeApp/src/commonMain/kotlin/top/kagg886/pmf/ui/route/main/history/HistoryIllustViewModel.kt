package top.kagg886.pmf.ui.route.main.history

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.IllustFetchViewModel

class HistoryIllustViewModel : IllustFetchViewModel(), KoinComponent {
    private val database by inject<AppDatabase>()
    override fun initInfinityRepository(): InfinityRepository<Illust> = object : InfinityRepository<Illust>() {
        private var page: Int = 1
        override suspend fun onFetchList(): List<Illust> = database.illustHistoryDAO().getByPage(page).map { it.illust }.apply {
            page++
        }
    }
}
