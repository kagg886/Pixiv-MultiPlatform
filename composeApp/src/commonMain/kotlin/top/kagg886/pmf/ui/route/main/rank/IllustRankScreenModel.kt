package top.kagg886.pmf.ui.route.main.rank

import top.kagg886.pixko.module.illust.RankCategory
import top.kagg886.pixko.module.illust.getRankIllust
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

class IllustRankScreenModel(val type: RankCategory) : IllustFetchViewModel() {
    override fun source() = flowOf(30) { params ->
        params.page { i -> client.getRankIllust(mode = type, page = i) }
    }
}
