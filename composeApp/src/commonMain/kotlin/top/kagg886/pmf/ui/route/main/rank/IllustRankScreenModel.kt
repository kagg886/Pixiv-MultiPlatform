package top.kagg886.pmf.ui.route.main.rank

import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.RankCategory
import top.kagg886.pixko.module.illust.getRankIllust
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.IllustFetchViewModel

class IllustRankScreenModel(val type: RankCategory) : IllustFetchViewModel() {
    override fun initInfinityRepository(): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>() {
            private var page = 1
            override suspend fun onFetchList(): List<Illust> {
                val res = client.getRankIllust(mode = type, page = page)
                page++
                return res
            }
        }
    }
}
