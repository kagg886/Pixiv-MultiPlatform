package top.kagg886.pmf.ui.route.main.detail.author

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserInfo
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.util.container

class AuthorScreenModel(val id:Int) : ContainerHost<AuthorScreenState, AuthorScreenSideEffect>, ViewModel(), ScreenModel,
    KoinComponent {
    override val container: Container<AuthorScreenState, AuthorScreenSideEffect> = container(AuthorScreenState.Loading) {
        loadUserById(id)
    }
    private val token by inject<PixivTokenStorage>()
    private val client = PixivAccountFactory.newAccountFromConfig {
        storage = token
    }

    fun loadUserById(id: Int, silent: Boolean = true) = intent {
        if (silent) {
            reduce { AuthorScreenState.Loading }
        }
        val illust = kotlin.runCatching {
            client.getUserInfo(id)
        }
        if (illust.isFailure) {
            if (silent) {
                reduce { AuthorScreenState.Error }
            }
            return@intent
        }
        reduce { AuthorScreenState.Success(illust.getOrThrow()) }
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