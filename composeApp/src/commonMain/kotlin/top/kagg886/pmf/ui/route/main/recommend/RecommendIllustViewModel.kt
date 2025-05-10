package top.kagg886.pmf.ui.route.main.recommend

import top.kagg886.pixko.module.illust.getRecommendIllust
import top.kagg886.pixko.module.illust.getRecommendIllustNext
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.next

class RecommendIllustViewModel : IllustFetchViewModel() {
    override fun source() = flowOf(30) { params ->
        params.next(
            { client.getRecommendIllust() },
            { ctx -> client.getRecommendIllustNext(ctx) },
            { ctx -> ctx.illusts },
        )
    }
}
