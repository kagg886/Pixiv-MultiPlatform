package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserNovel
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

@Composable
fun AuthorScreen.AuthorNovel(user: UserInfo) {
    val model = rememberScreenModel(tag = "user_novel_${user.user.id}") {
        AuthorNovelViewModel(user.user.id.toLong())
    }
    NovelFetchScreen(model)
}

class AuthorNovelViewModel(val id: Long) : NovelFetchViewModel() {
    override fun source() = flowOf(20) { params ->
        params.page { i -> client.getUserNovel(id, i) }
    }
}
