package top.kagg886.pmf.ui.route.main.detail.illust

import top.kagg886.pixko.module.illust.getRelatedIllust
import top.kagg886.pixko.module.illust.getRelatedIllustNext
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.next

class IllustSimilarViewModel(private val illust: Long) : IllustFetchViewModel() {
    override fun source() = flowOf(30) { params ->
        params.next(
            { client.getRelatedIllust(illust) },
            { ctx -> client.getRelatedIllustNext(ctx) },
            { ctx -> ctx.illusts },
        )
    }
}
