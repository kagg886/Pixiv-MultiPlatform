package top.kagg886.pmf.ui.route.main.rank

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import top.kagg886.pixko.module.illust.RankCategory
import top.kagg886.pmf.NavigationItem
import top.kagg886.pmf.composeWithAppBar
import top.kagg886.pmf.res.*
import top.kagg886.pmf.res.allStringResources
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.util.stringResource

class RankScreen : Screen {
    @Composable
    override fun Content() = NavigationItem.RANK.composeWithAppBar {
        RankScreen()
    }
}

@Composable
private fun Screen.RankScreen() {
    val page = rememberScreenModel {
        object : ScreenModel {
            val page = mutableIntStateOf(0)
        }
    }
    val index by page.page
    TabContainer(
        modifier = Modifier.fillMaxSize(),
        tab = RankCategory.entries,
        tabTitle = { Text(stringResource(Res.allStringResources["rank_${it.content}"]!!)) },
        current = RankCategory.entries[index],
        scrollable = true,
        onCurrentChange = { page.page.value = RankCategory.entries.indexOf(it) },
    ) {
        val model = rememberScreenModel(it.toString()) {
            IllustRankScreenModel(it)
        }
        IllustFetchScreen(model)
    }
}
