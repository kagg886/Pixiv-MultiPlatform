package top.kagg886.pmf.ui.route.main.space

import top.kagg886.pixko.module.illust.getLatestIllust
import top.kagg886.pixko.module.illust.getLatestIllustNext
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.next

class NewestIllustViewModel : IllustFetchViewModel() {
    override fun source() = flowOf(30) { params ->
        params.next(
            { client.getLatestIllust() },
            { ctx -> client.getLatestIllustNext(ctx) },
            { ctx -> ctx.illusts },
        )
    }
}
