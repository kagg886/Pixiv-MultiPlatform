package top.kagg886.pmf.ui.route.main.detail.author

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.user.*
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.container

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
        val illust = kotlin.runCatching {
            client.getUserInfo(id)
        }
        if (illust.isFailure) {
            illust.exceptionOrNull()!!.printStackTrace()
            if (silent) {
                reduce { AuthorScreenState.Error }
            }
            return@intent
        }
        reduce { AuthorScreenState.Success(illust.getOrThrow()) }
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
                postSideEffect(AuthorScreenSideEffect.Toast("关注失败~"))
                return@runOn
            }
            if (private) {
                postSideEffect(AuthorScreenSideEffect.Toast("悄悄关注是不想让别人看到嘛⁄(⁄ ⁄•⁄ω⁄•⁄ ⁄)⁄"))
            } else {
                postSideEffect(AuthorScreenSideEffect.Toast("关注成功~"))
            }
            reduce {
                state.copy(
                    user = state.user.copy(
                        user = state.user.user.copy(
                            isFollowed = true,
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
                postSideEffect(AuthorScreenSideEffect.Toast("取关失败~(*^▽^*)"))
                return@runOn
            }
            postSideEffect(AuthorScreenSideEffect.Toast("取关成功~o(╥﹏╥)o"))
            reduce {
                state.copy(
                    user = state.user.copy(
                        user = state.user.user.copy(
                            isFollowed = false,
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
