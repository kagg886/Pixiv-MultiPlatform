package top.kagg886.pmf.ui.route.main.space

import top.kagg886.pixko.module.illust.getIllustFollowList
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

class SpaceIllustViewModel : IllustFetchViewModel() {
    override fun source() = flowOf(30) { params ->
        params.page { i -> client.getIllustFollowList { page = i } }
    }
}
