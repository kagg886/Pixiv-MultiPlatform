package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.identity
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.runningReduce
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.User
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.unFollowUser
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.follow_fail
import top.kagg886.pmf.follow_success
import top.kagg886.pmf.follow_success_private
import top.kagg886.pmf.un_bookmark_failed
import top.kagg886.pmf.un_bookmark_success

private typealias FU = (PagingData<User>) -> PagingData<User>

abstract class AuthorFetchViewModel : ContainerHost<AuthorFetchViewState, AuthorFetchSideEffect>, ViewModel(), ScreenModel {
    protected val client = PixivConfig.newAccountFromConfig()
    private val refreshSignal = MutableSharedFlow<Unit>()
    private val transforms = MutableSharedFlow<FU>()
    abstract fun source(): Flow<PagingData<User>>
    override val container: Container<AuthorFetchViewState, AuthorFetchSideEffect> = container(AuthorFetchViewState())

    val data = merge(flowOf(Unit), refreshSignal).flatMapLatest {
        source().cachedIn(viewModelScope).let { cached ->
            merge(flowOf(::identity), transforms).runningReduce { a, b -> { v -> b(a(v)) } }
                .flatMapLatest { f -> cached.map(f) }
        }
    }.cachedIn(viewModelScope)

    fun refresh() = intent { refreshSignal.emit(Unit) }

    @OptIn(OrbitExperimental::class)
    fun followUser(author: User, private: Boolean = false) = intent {
        runOn<AuthorFetchViewState> {
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
            transforms.emit { data ->
                data.map {
                    if (it.id == author.id) {
                        it.copy(isFollowed = true)
                    } else {
                        it
                    }
                }
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun unFollowUser(author: User) = intent {
        runOn<AuthorFetchViewState> {
            val result = kotlin.runCatching {
                client.unFollowUser(author.id)
            }

            if (result.isFailure) {
                postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.un_bookmark_failed)))
                return@runOn
            }
            postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.un_bookmark_success)))
            transforms.emit { data ->
                data.map {
                    if (it == author) {
                        it.copy(isFollowed = false)
                    } else {
                        it
                    }
                }
            }
        }
    }
}

data class AuthorFetchViewState(val scrollerState: LazyListState = LazyListState())

sealed class AuthorFetchSideEffect {
    data class Toast(val msg: String) : AuthorFetchSideEffect()
}
