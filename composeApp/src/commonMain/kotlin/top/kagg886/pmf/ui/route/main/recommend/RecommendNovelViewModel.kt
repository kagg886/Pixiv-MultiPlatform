package top.kagg886.pmf.ui.route.main.recommend

import top.kagg886.pixko.module.novel.getRecommendNovel
import top.kagg886.pixko.module.novel.getRecommendNovelNext
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.next

class RecommendNovelViewModel : NovelFetchViewModel() {
    override fun source() = flowOf(20) { params ->
        params.next(
            { client.getRecommendNovel() },
            { ctx -> client.getRecommendNovelNext(ctx) },
            { ctx -> ctx.novels },
        )
    }
}
