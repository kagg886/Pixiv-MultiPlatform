package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.User
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.unFollowUser
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.follow_fail
import top.kagg886.pmf.follow_success
import top.kagg886.pmf.follow_success_private
import top.kagg886.pmf.un_bookmark_failed
import top.kagg886.pmf.un_bookmark_success

abstract class AuthorFetchViewModel : ContainerHost<AuthorFetchViewState, AuthorFetchSideEffect>, ViewModel(), ScreenModel {
    protected val client = PixivConfig.newAccountFromConfig()

    private lateinit var repo: InfinityRepository<User>

    override val container: Container<AuthorFetchViewState, AuthorFetchSideEffect> =
        container(AuthorFetchViewState.Loading) {
            loading()
        }

    abstract fun initInfinityRepository(): InfinityRepository<User>

    fun loading(pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                AuthorFetchViewState.Loading
            }
        }
        repo = initInfinityRepository()
        val list = repo.take(20).toList()
        reduce { AuthorFetchViewState.ShowAuthorList(list, noMoreData = repo.noMoreData) }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMore() = intent {
        runOn<AuthorFetchViewState.ShowAuthorList> {
            val list = state.data + repo.take(20).toList()
            reduce { state.copy(data = list, noMoreData = repo.noMoreData) }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun followUser(author: User, private: Boolean = false) = intent {
        runOn<AuthorFetchViewState.ShowAuthorList> {
            val result = kotlin.runCatching {
                client.followUser(author.id, if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC)
            }

            if (result.isFailure) {
                postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.follow_fail)))
                return@runOn
            }
            if (private) {
                postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.follow_success_private)))
            } else {
                postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.follow_success)))
            }
            reduce {
                state.copy(
                    data = state.data.map {
                        if (it.id == author.id) {
                            it.copy(isFollowed = true)
                        } else {
                            it
                        }
                    },
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun unFollowUser(author: User) = intent {
        runOn<AuthorFetchViewState.ShowAuthorList> {
            val result = kotlin.runCatching {
                client.unFollowUser(author.id)
            }

            if (result.isFailure) {
                postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.un_bookmark_failed)))
                return@runOn
            }
            postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.un_bookmark_success)))
            reduce {
                state.copy(
                    data = state.data.map {
                        if (it == author) {
                            it.copy(isFollowed = false)
                        } else {
                            it
                        }
                    },
                )
            }
        }
    }
}

sealed class AuthorFetchViewState {
    data object Loading : AuthorFetchViewState()
    data class ShowAuthorList(
        val data: List<User>,
        val noMoreData: Boolean = false,
        val scrollerState: LazyListState = LazyListState(),
    ) : AuthorFetchViewState()
}

sealed class AuthorFetchSideEffect {
    data class Toast(val msg: String) : AuthorFetchSideEffect()
}
