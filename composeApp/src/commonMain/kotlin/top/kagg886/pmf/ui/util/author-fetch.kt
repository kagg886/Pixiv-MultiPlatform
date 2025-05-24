package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
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

abstract class AuthorFetchViewModel : ContainerHost<AuthorFetchViewState, AuthorFetchSideEffect>, ViewModel(), ScreenModel {
    protected val client = PixivConfig.newAccountFromConfig()
    private val signal = MutableSharedFlow<Unit>()
    abstract fun source(): Flow<PagingData<User>>
    override val container: Container<AuthorFetchViewState, AuthorFetchSideEffect> = container(AuthorFetchViewState())

    val data = merge(flowOf(Unit), signal).flatMapLatestScoped { scope, _ ->
        userRouter.intercept(source().cachedIn(scope))
    }.cachedIn(viewModelScope)

    fun refresh() = intent { signal.emit(Unit) }

    @OptIn(OrbitExperimental::class)
    fun followUser(author: User, private: Boolean = false) = intent {
        runOn<AuthorFetchViewState> {
            val result = runCatching { client.followUser(author.id, if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC) }

            if (result.isFailure) {
                postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.follow_fail)))
                return@runOn
            }
            if (private) {
                postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.follow_success_private)))
            } else {
                postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.follow_success)))
            }
            userRouter.push { u -> if (u.id == author.id) u.copy(isFollowed = true) else u }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun unFollowUser(author: User) = intent {
        runOn<AuthorFetchViewState> {
            val result = runCatching { client.unFollowUser(author.id) }

            if (result.isFailure) {
                postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.un_bookmark_failed)))
                return@runOn
            }
            postSideEffect(AuthorFetchSideEffect.Toast(getString(Res.string.un_bookmark_success)))
            userRouter.push { u -> if (u.id == author.id) u.copy(isFollowed = false) else u }
        }
    }
}

data class AuthorFetchViewState(val scrollerState: LazyListState = LazyListState())

sealed class AuthorFetchSideEffect {
    data class Toast(val msg: String) : AuthorFetchSideEffect()
}
