package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.User
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.getFollowingList
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.AuthorCard
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.ui.util.container

@Composable
fun AuthorScreen.AuthorFollow(user: UserInfo) {
    val model = rememberScreenModel("user_follow_${user.user.id}") {
        AuthorFollowViewModel(user.user.id)
    }
    val state by model.collectAsState()

    val snack = LocalSnackBarHost.current

    model.collectSideEffect {
        when (it) {
            is AuthorFollowSideEffect.Toast -> snack.showSnackbar(it.msg)
        }
    }
    AuthorFollowScreenContent(state, model)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthorFollowScreenContent(state: AuthorFollowState, model: AuthorFollowViewModel) {
    when (state) {
        is AuthorFollowState.Loading -> {
            Loading()
        }

        is AuthorFollowState.Success -> {
            val scroll = state.scrollerState

            val scope = rememberCoroutineScope()

            var refresh by remember {
                mutableStateOf(false)
            }

            PullToRefreshBox(
                isRefreshing = refresh,
                onRefresh = {
                    refresh = true
                    scope.launch {
                        model.loading(true).join()
                    }.invokeOnCompletion {
                        refresh = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.data.isEmpty()) {
                    ErrorPage(text = "页面为空") {
                        scope.launch {
                            model.loading()
                        }
                    }
                    return@PullToRefreshBox
                }
                LazyColumn {
                    items(state.data, key = { it.id }) {
                        AuthorCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
                            user = it
                        ) { isRequestFavorite ->
                            if (isRequestFavorite) {
                                model.followUser(it.id).join()
                            } else {
                                model.unFollowUser(it.id).join()
                            }
                        }
                    }

                    item {
                        LaunchedEffect(Unit) {
                            if (!state.noMoreData) {
                                model.loadMore()
                            }
                        }
                        if (!state.noMoreData) {
                            Loading()
                            return@item
                        }
                        Text(
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            text = "没有更多了"
                        )
                    }
                }
                AnimatedVisibility(
                    visible = scroll.canScrollBackward,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                scroll.animateScrollToItem(0)
                            }
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, null)
                    }
                }
            }

        }
    }
}

private class AuthorFollowViewModel(val user: Int) : ContainerHost<AuthorFollowState, AuthorFollowSideEffect>, ViewModel(), ScreenModel {
    private val client = PixivConfig.newAccountFromConfig()

    private var repo: InfinityRepository<User>? = null

    override val container: Container<AuthorFollowState, AuthorFollowSideEffect> =
        container(AuthorFollowState.Loading) {
            loading()
        }

    fun loading(pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                AuthorFollowState.Loading
            }
        }
        repo = object : InfinityRepository<User>() {
            var i = 1
            override suspend fun onFetchList(): List<User>? {
                val result = kotlin.runCatching {
                    client.getFollowingList(user) {
                        page = i
                    }
                }
                if (result.isFailure) {
                    return null
                }
                i++
                return result.getOrThrow()
            }

        }
        reduce {
            AuthorFollowState.Success(
                repo!!.take(20).toList(),
                noMoreData = repo!!.noMoreData
            )
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMore() = intent {
        runOn<AuthorFollowState.Success> {
            val result = repo!!.take(20).toList()
            reduce {
                AuthorFollowState.Success(
                    state.data + result,
                    noMoreData = repo!!.noMoreData
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun followUser(userId: Int) = intent {
        runOn<AuthorFollowState.Success> {
            val result = kotlin.runCatching {
                client.followUser(userId)
            }
            if (result.isFailure) {
                postSideEffect(AuthorFollowSideEffect.Toast("关注失败~"))
                return@runOn
            }
            postSideEffect(AuthorFollowSideEffect.Toast("关注成功~"))
            reduce {
                state.copy(
                    data = state.data.map {
                        if (it.id == userId) {
                            it.copy(isFollowed = true)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun unFollowUser(userId: Int) = intent {
        runOn<AuthorFollowState.Success> {
            val result = kotlin.runCatching {
                client.followUser(userId)
            }
            if (result.isFailure) {
                postSideEffect(AuthorFollowSideEffect.Toast("取关失败~(*^▽^*)"))
                return@runOn
            }
            postSideEffect(AuthorFollowSideEffect.Toast("取关成功~o(╥﹏╥)o"))
            reduce {
                state.copy(
                    data = state.data.map {
                        if (it.id == userId) {
                            it.copy(isFollowed = false)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }

}

private sealed class AuthorFollowState {
    data object Loading : AuthorFollowState()
    data class Success(
        val data: List<User>,
        val noMoreData: Boolean = false,
        val scrollerState: LazyListState = LazyListState()
    ) : AuthorFollowState()
}

private sealed class AuthorFollowSideEffect {
    data class Toast(val msg: String) : AuthorFollowSideEffect()
}