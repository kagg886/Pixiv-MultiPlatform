package top.kagg886.pmf.ui.route.main.space

import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.getIllustFollowList
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.IllustFetchViewModel

class SpaceIllustViewModel : IllustFetchViewModel() {
    override fun initInfinityRepository(): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>() {
            private var it = 1
            override suspend fun onFetchList(): List<Illust>? {
                val result = client.getIllustFollowList {
                    page = it
                }
                it++
                return result
            }
        }
    }
}
