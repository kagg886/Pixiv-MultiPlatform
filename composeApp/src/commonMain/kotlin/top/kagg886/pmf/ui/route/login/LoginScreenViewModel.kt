package top.kagg886.pmf.ui.route.login

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.serialization.encodeValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.PixivVerification
import top.kagg886.pixko.module.user.getCurrentUserSimpleProfile
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.util.container
import kotlin.time.Duration.Companion.seconds

class LoginScreenViewModel : ContainerHost<LoginViewState, LoginSideEffect>, ViewModel(), ScreenModel, KoinComponent {
    private val storage by inject<PixivTokenStorage>()

    override val container: Container<LoginViewState, LoginSideEffect> =
        container(LoginViewState.Loading("正在加载中...")) {
           init()
        }

    fun init() = intent {
        if (!storage.haveToken()) {
            if (currentPlatform is Platform.Desktop) {
                withContext(Dispatchers.IO) {
                    initKCEF()
                }
                return@intent
            }
            reduce {
                LoginViewState.WaitLogin
            }
            return@intent
        }
        postSideEffect(LoginSideEffect.NavigateToMain)
    }


    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    fun verifyPixivAccount(factory: PixivVerification, url: String) = intent {
        reduce {
            LoginViewState.Loading("正在解析配置文件")
        }
        val account = factory.verify(url) {
            this.storage = this@LoginScreenViewModel.storage
        }
        val user = kotlin.runCatching {
            account.getCurrentUserSimpleProfile()
        }
        if (user.isFailure) {
            postSideEffect(LoginSideEffect.Toast("验证账号状态时出了点问题，请重新登录。"))
            reduce {
                LoginViewState.WaitLogin
            }
        }
        val u = user.getOrThrow()
        PixivConfig.pixiv_user = u
        reduce {
            LoginViewState.Loading("欢迎您！ ${u.name}")
        }
        delay(3.seconds)
        postSideEffect(LoginSideEffect.NavigateToMain)
    }
}

expect fun LoginScreenViewModel.initKCEF(): Job

sealed class LoginViewState {
    data class Loading(val msg: String) : LoginViewState()
    data object WaitLogin : LoginViewState()
}

sealed class LoginSideEffect {
    data object NavigateToMain : LoginSideEffect()
    data class Toast(val msg: String) : LoginSideEffect()
}