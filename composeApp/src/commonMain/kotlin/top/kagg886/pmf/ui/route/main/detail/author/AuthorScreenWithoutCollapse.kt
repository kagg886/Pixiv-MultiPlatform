package top.kagg886.pmf.ui.route.main.detail.author

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.res.*
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorFollow
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorIllust
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorIllustBookmark
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorNovel
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorNovelBookmark
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorProfile
import top.kagg886.pmf.util.stringResource

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

    @Composable
    override fun AuthorContent(state: AuthorScreenState) {
        val model = rememberScreenModel {
            AuthorScreenModel(id)
        }
        when (state) {
            AuthorScreenState.Error -> {
                ErrorPage(text = stringResource(Res.string.load_failed), showBackButton = true) {
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
                val data = linkedMapOf<String, @Composable () -> Unit>(
                    stringResource(Res.string.personal_profile) to {
                        AuthorProfile(state.user)
                    },
                    stringResource(Res.string.illustration_works) to {
                        AuthorIllust(state.user)
                    },
                    stringResource(Res.string.novel_works) to {
                        AuthorNovel(state.user)
                    },
                    stringResource(Res.string.illustration_bookmarks) to {
                        AuthorIllustBookmark(state.user)
                    },
                    stringResource(Res.string.novel_bookmarks) to {
                        AuthorNovelBookmark(state.user)
                    },
                    stringResource(Res.string.follow) to {
                        AuthorFollow(state.user)
                    },
                ).toList()
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
