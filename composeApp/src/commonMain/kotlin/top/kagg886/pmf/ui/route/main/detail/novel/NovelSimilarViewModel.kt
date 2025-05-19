package top.kagg886.pmf.ui.route.main.detail.novel

import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.getRelatedNovel
import top.kagg886.pixko.module.novel.getRelatedNovelNext
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.next

class NovelSimilarViewModel(private val novel: Long): NovelFetchViewModel()  {
    override fun source() = flowOf(30) { params ->
        params.next(
            { client.getRelatedNovel(novel) },
            { ctx -> client.getRelatedNovelNext(ctx) },
            { ctx -> ctx.novels },
        )
    }
}