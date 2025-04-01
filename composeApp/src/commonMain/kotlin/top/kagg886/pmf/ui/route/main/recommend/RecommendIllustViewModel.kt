package top.kagg886.pmf.ui.route.main.recommend

import kotlin.coroutines.CoroutineContext
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustResult
import top.kagg886.pixko.module.illust.getRecommendIllust
import top.kagg886.pixko.module.illust.getRecommendIllustNext
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.IllustFetchViewModel

class RecommendIllustViewModel : IllustFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>(coroutineContext) {
            private var context: IllustResult? = null
            override suspend fun onFetchList(): List<Illust>? {
                context = if (context == null) {
                    client.getRecommendIllust()
                } else {
                    client.getRecommendIllustNext(context!!)
                }
                return context?.illusts
            }
        }
    }
}
