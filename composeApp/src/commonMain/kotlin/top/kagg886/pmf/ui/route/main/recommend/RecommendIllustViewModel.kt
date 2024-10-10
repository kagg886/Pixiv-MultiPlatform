package top.kagg886.pmf.ui.route.main.recommend

import top.kagg886.pixko.module.illust.*
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import kotlin.coroutines.CoroutineContext

class RecommendIllustViewModel : IllustFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>(coroutineContext) {
            private var context: IllustResult? = null
            override suspend fun onFetchList(): List<Illust>? {
                kotlin.runCatching {
                    context = if (context == null) {
                        client.getRecommendIllust()
                    } else {
                        client.getRecommendIllustNext(context!!)
                    }
                }
                return context?.illusts
            }
        }
    }
}