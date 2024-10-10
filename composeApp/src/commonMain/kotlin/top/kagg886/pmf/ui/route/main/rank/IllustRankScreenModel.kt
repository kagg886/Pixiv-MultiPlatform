package top.kagg886.pmf.ui.route.main.rank

import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.RankCategory
import top.kagg886.pixko.module.illust.getRankIllust
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import kotlin.coroutines.CoroutineContext

class IllustRankScreenModel(val type: RankCategory) : IllustFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>(coroutineContext) {
            private var page = 1
            override suspend fun onFetchList(): List<Illust>? {
                val res = kotlin.runCatching {
                    client.getRankIllust(mode = type, page = ++page)
                }
                if (res.isFailure) {
                    return null
                }
                return res.getOrThrow()
            }
        }
    }

}