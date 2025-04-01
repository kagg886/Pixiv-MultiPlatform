package top.kagg886.pmf.ui.route.main.detail.author

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.route.main.detail.author.tabs.*
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect

class AuthorScreenWithoutCollapse(override val id: Int) : AuthorScreen(id) {
    @Composable
    override fun Content() {
        val model = rememberScreenModel {
            AuthorScreenModel(id)
        }
        val state by model.collectAsState()

        val host = LocalSnackBarHost.current
        model.collectSideEffect {
            when (it) {
                is AuthorScreenSideEffect.Toast -> host.showSnackbar(it.msg)
            }
        }
        Box(Modifier.fillMaxSize()) {
            AuthorContent(state)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun AuthorContent(state: AuthorScreenState) {
        val model = rememberScreenModel {
            AuthorScreenModel(id)
        }
        when (state) {
            AuthorScreenState.Error -> {
                ErrorPage(text = "加载失败", showBackButton = true) {
                    model.loadUserById(id)
                }
            }

            AuthorScreenState.Loading -> {
                Loading()
            }

            is AuthorScreenState.Success -> {
                val key = remember {
                    mutableIntStateOf(0)
                }
                val data = remember {
                    linkedMapOf<String, @Composable () -> Unit>(
                        "个人信息" to {
                            AuthorProfile(state.user)
                        },
                        "插画作品" to {
                            AuthorIllust(state.user)
                        },
                        "小说作品" to {
                            AuthorNovel(state.user)
                        },
                        "插画收藏" to {
                            AuthorIllustBookmark(state.user)
                        },
                        "小说收藏" to {
                            AuthorNovelBookmark(state.user)
                        },
                        "关注" to {
                            AuthorFollow(state.user)
                        },
                    ).toList()
                }
                TabContainer(
                    state = key,
                    tab = data.map { it.first },
                    scrollable = true,
                ) {
                    data[it].second.invoke()
                }
            }
        }
    }
}
