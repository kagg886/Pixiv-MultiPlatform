package top.kagg886.pmf.ui.route.main.detail.author

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.getUserInfo
import top.kagg886.pixko.module.user.unFollowUser
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.follow_fail
import top.kagg886.pmf.follow_success
import top.kagg886.pmf.follow_success_private
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.unfollow_fail
import top.kagg886.pmf.unfollow_success
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.util.logger

class AuthorScreenModel(val id: Int) :
    ContainerHost<AuthorScreenState, AuthorScreenSideEffect>,
    ViewModel(),
    ScreenModel,
    KoinComponent {
    override val container: Container<AuthorScreenState, AuthorScreenSideEffect> =
        container(AuthorScreenState.Loading) {
            loadUserById(id)
        }
    private val client = PixivConfig.newAccountFromConfig()

    fun loadUserById(id: Int, silent: Boolean = true) = intent {
        if (silent) {
            reduce { AuthorScreenState.Loading }
        }
        val info = kotlin.runCatching {
            client.getUserInfo(id)
        }
        if (info.isFailure) {
            logger.w("failed to get author info", info.exceptionOrNull())
            if (silent) {
                reduce { AuthorScreenState.Error }
            }
            return@intent
        }
        reduce { AuthorScreenState.Success(info.getOrThrow()) }
    }

    @OptIn(OrbitExperimental::class)
    fun followUser(private: Boolean = false) = intent {
        runOn<AuthorScreenState.Success> {
            val result = kotlin.runCatching {
                client.followUser(
                    state.user.user.id,
                    if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC,
                )
            }
            if (result.isFailure) {
                postSideEffect(AuthorScreenSideEffect.Toast(getString(Res.string.follow_fail)))
                return@runOn
            }
            if (private) {
                postSideEffect(AuthorScreenSideEffect.Toast(getString(Res.string.follow_success_private)))
            } else {
                postSideEffect(AuthorScreenSideEffect.Toast(getString(Res.string.follow_success)))
            }
            reduce {
                state.copy(
                    user = state.user.copy(
                        user = state.user.user.copy(
                            isFollowed = true,
                        ),
                        profile = state.user.profile.copy(
                            totalFollowUsers = state.user.profile.totalFollowUsers + 1,
                        ),
                    ),
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun unFollowUser() = intent {
        runOn<AuthorScreenState.Success> {
            val result = kotlin.runCatching {
                client.unFollowUser(state.user.user.id)
            }
            if (result.isFailure) {
                postSideEffect(AuthorScreenSideEffect.Toast(getString(Res.string.unfollow_fail)))
                return@runOn
            }
            postSideEffect(AuthorScreenSideEffect.Toast(getString(Res.string.unfollow_success)))
            reduce {
                state.copy(
                    user = state.user.copy(
                        user = state.user.user.copy(
                            isFollowed = false,
                        ),
                        profile = state.user.profile.copy(
                            totalFollowUsers = state.user.profile.totalFollowUsers - 1,
                        ),
                    ),
                )
            }
        }
    }
}

sealed class AuthorScreenState {
    data object Loading : AuthorScreenState()
    data object Error : AuthorScreenState()
    data class Success(val user: UserInfo, val initPage: Int = 0) : AuthorScreenState()
}

sealed class AuthorScreenSideEffect {
    data class Toast(val msg: String) : AuthorScreenSideEffect()
}
