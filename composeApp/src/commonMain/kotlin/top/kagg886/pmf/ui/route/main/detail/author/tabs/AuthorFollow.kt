package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import kotlin.coroutines.CoroutineContext
import top.kagg886.pixko.User
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getFollowingList
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.AuthorFetchScreen
import top.kagg886.pmf.ui.util.AuthorFetchViewModel

@Composable
fun AuthorScreen.AuthorFollow(user: UserInfo) {
    val model = rememberScreenModel("user_follow_${user.user.id}") {
        AuthorFollowViewModel(user.user.id)
    }
    AuthorFetchScreen(model)
}

private class AuthorFollowViewModel(val user: Int) : AuthorFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<User> {
        return object : InfinityRepository<User>() {
            var i = 1
            override suspend fun onFetchList(): List<User> {
                val result = client.getFollowingList(user) {
                    page = i
                }

                i++
                return result
            }
        }
    }
}
