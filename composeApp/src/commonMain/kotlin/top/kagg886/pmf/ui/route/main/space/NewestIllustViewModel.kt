package top.kagg886.pmf.ui.route.main.space

import kotlin.coroutines.CoroutineContext
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustResult
import top.kagg886.pixko.module.illust.getLatestIllust
import top.kagg886.pixko.module.illust.getLatestIllustNext
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.IllustFetchViewModel

class NewestIllustViewModel : IllustFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>() {
            private var ctx: IllustResult? = null
            override suspend fun onFetchList(): List<Illust>? {
                ctx = if (ctx == null) client.getLatestIllust() else client.getLatestIllustNext(ctx!!)
                return ctx?.illusts
            }
        }
    }
}
