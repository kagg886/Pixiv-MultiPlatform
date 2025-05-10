package top.kagg886.pmf.ui.route.main.recommend

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.NovelResult
import top.kagg886.pixko.module.novel.getRecommendNovel
import top.kagg886.pixko.module.novel.getRecommendNovelNext
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.catch
import top.kagg886.pmf.ui.util.next

class RecommendNovelViewModel : NovelFetchViewModel() {
    override fun source() = Pager(PagingConfig(20)) {
        object : PagingSource<NovelResult, Novel>() {
            override fun getRefreshKey(state: PagingState<NovelResult, Novel>) = null
            override suspend fun load(params: LoadParams<NovelResult>) = catch {
                params.next(
                    { client.getRecommendNovel() },
                    { ctx -> client.getRecommendNovelNext(ctx) },
                    { ctx -> ctx.novels },
                )
            }
        }
    }.flow
}
