package top.kagg886.pmf.ui.route.main.rank

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import top.kagg886.pixko.module.illust.RankCategory
import top.kagg886.pmf.NavigationItem
import top.kagg886.pmf.composeWithAppBar
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.IllustFetchScreen

object RankScreen : Screen {
    private class PageScreenModel : ScreenModel {
        val page: MutableState<Int> = mutableIntStateOf(0)
    }

    @Composable
    override fun Content() = composeWithAppBar(NavigationItem.RANK) {
        val page = rememberScreenModel {
            PageScreenModel()
        }
        TabContainer(
            modifier = Modifier.fillMaxSize(),
            tab = listOf("日榜", "周榜", "月榜", "男性", "女性", "原创", "新人"),
            scrollable = true,
            state = page.page,
        ) {
            val i = RankCategory.entries[it]
            IllustContent(rank = i)
        }
    }

    @Composable
    private fun IllustContent(rank: RankCategory) {
        val model = rememberScreenModel(tag = "rank_$rank") {
            IllustRankScreenModel(type = rank)
        }
        IllustFetchScreen(model)
    }
}
