package top.kagg886.pmf.ui.route.main.space

import top.kagg886.pixko.module.illust.*
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import kotlin.coroutines.CoroutineContext

class NewestIllustViewModel : IllustFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>() {
            private var ctx: IllustResult? = null
            override suspend fun onFetchList(): List<Illust>? {
                kotlin.runCatching {
                    ctx = if (ctx == null) client.getLatestIllust() else client.getLatestIllustNext(ctx!!)
                }
                return ctx?.illusts
            }

        }
    }
}