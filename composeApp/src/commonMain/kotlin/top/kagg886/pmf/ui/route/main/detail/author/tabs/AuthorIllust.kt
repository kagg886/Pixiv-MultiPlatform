package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserIllust
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

@Composable
fun AuthorScreen.AuthorIllust(user: UserInfo) {
    val model = rememberScreenModel(tag = "user_illust_${user.user.id}") {
        AuthorIllustViewModel(user.user.id)
    }
    IllustFetchScreen(model)
}

private class AuthorIllustViewModel(val user: Int) : IllustFetchViewModel() {
    override fun source() = flowOf(30) { params ->
        params.page { i -> client.getUserIllust(user.toLong(), i) }
    }
}
