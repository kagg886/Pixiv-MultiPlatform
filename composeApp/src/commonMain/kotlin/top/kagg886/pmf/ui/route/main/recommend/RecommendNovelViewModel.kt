package top.kagg886.pmf.ui.route.main.recommend

import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.NovelResult
import top.kagg886.pixko.module.novel.getRecommendNovel
import top.kagg886.pixko.module.novel.getRecommendNovelNext
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import kotlin.coroutines.CoroutineContext

class RecommendNovelViewModel : NovelFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Novel> {
        return object : InfinityRepository<Novel>(coroutineContext) {
            private var context: NovelResult? = null
            override suspend fun onFetchList(): List<Novel>? {
                kotlin.runCatching {
                    context = if (context == null) {
                        client.getRecommendNovel()
                    } else {
                        client.getRecommendNovelNext(context!!)
                    }
                }
                return context?.novels
            }
        }
    }

}