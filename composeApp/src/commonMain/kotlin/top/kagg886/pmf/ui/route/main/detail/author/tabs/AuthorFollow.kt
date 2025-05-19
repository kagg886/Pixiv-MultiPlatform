package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getFollowingList
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.AuthorFetchScreen
import top.kagg886.pmf.ui.util.AuthorFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

@Composable
fun AuthorScreen.AuthorFollow(user: UserInfo) {
    val model = rememberScreenModel("user_follow_${user.user.id}") {
        AuthorFollowViewModel(user.user.id)
    }
    AuthorFetchScreen(model)
}

private class AuthorFollowViewModel(val user: Int) : AuthorFetchViewModel() {
    override fun source() = flowOf(20) { params ->
        params.page { i -> client.getFollowingList(user) { page = i } }
    }
}
