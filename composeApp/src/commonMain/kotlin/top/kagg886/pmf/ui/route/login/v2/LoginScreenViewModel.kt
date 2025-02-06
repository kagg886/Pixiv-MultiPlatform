package top.kagg886.pmf.ui.route.login.v2

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.PixivVerification
import top.kagg886.pixko.TokenType
import top.kagg886.pixko.module.user.getCurrentUserSimpleProfile
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.route.login.v2.LoginType.*
import top.kagg886.pmf.ui.util.container
import kotlin.time.Duration.Companion.seconds

class LoginScreenViewModel : ContainerHost<LoginViewState, LoginSideEffect>, ViewModel(), ScreenModel, KoinComponent {
    private val storage by inject<PixivTokenStorage>()
    override val container: Container<LoginViewState, LoginSideEffect> = container(LoginViewState.WaitChooseLogin)


    @OptIn(OrbitExperimental::class)
    fun selectLoginType(loginType: LoginType) = intent {
        runOn<LoginViewState.WaitChooseLogin> {
            when (loginType) {
                InputTokenLogin -> reduce {
                    LoginViewState.LoginType.InputTokenLogin
                }

                BrowserLogin -> {
                    if (currentPlatform is Platform.Desktop) {
                        initKCEF().join()
                    }
                }
            }
        }
    }

    fun challengeRefreshToken(token: String) = intent {
        reduce {
            LoginViewState.ProcessingUserData("正在检查token...")
        }
        val tempStorage = PixivTokenStorage().apply {
            this.setToken(TokenType.REFRESH, token)
        }

        val account = PixivAccountFactory.newAccountFromConfig {
            storage = tempStorage
        }

        val u = try {
            account.getCurrentUserSimpleProfile()
        } catch (e: Exception) {
            null
        }
        if (u == null) {
            postSideEffect(LoginSideEffect.Toast("验证token有效性时出现问题。请重新验证"))
            reduce {
                LoginViewState.LoginType.InputTokenLogin
            }
            return@intent
        }
        PixivConfig.pixiv_user = u
        storage.setToken(TokenType.ACCESS, tempStorage.getToken(TokenType.ACCESS)!!)
        storage.setToken(TokenType.REFRESH, tempStorage.getToken(TokenType.REFRESH)!!)

        reduce {
            LoginViewState.ProcessingUserData("欢迎您！ ${u.name}")
        }
        delay(3.seconds)
        postSideEffect(LoginSideEffect.NavigateToMain)
    }

    fun challengePixivLoginUrl(factory: PixivVerification, url: String) = intent {
        reduce { LoginViewState.ProcessingUserData("正在解析用户信息...") }
        val u = try {
            val account = factory.verify(url) {
                this.storage = this@LoginScreenViewModel.storage
            }
            account.getCurrentUserSimpleProfile()
        } catch (e: Exception) {
            null
        }

        if (u == null) {
            postSideEffect(LoginSideEffect.Toast("验证账号状态时出了点问题，请重新登录。"))
            reduce {
                LoginViewState.LoginType.BrowserLogin.ShowBrowser
            }
            return@intent
        }
        PixivConfig.pixiv_user = u
        reduce {
            LoginViewState.ProcessingUserData("欢迎您！ ${u.name}")
        }
        delay(3.seconds)
        postSideEffect(LoginSideEffect.NavigateToMain)

    }
}

enum class LoginType {
    InputTokenLogin,
    BrowserLogin
}

expect fun LoginScreenViewModel.initKCEF(): Job

sealed interface LoginViewState {
    data object WaitChooseLogin : LoginViewState

    sealed interface LoginType : LoginViewState {

        data object InputTokenLogin : LoginType

        sealed interface BrowserLogin : LoginType {
            data class Loading(val msg: String) : BrowserLogin
            data object ShowBrowser : BrowserLogin
        }
    }

    data class ProcessingUserData(val msg: String) : LoginViewState
}

sealed interface LoginSideEffect {
    data object NavigateToMain : LoginSideEffect
    data class Toast(val msg: String) : LoginSideEffect
}