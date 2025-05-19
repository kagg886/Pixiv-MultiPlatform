package top.kagg886.pmf.ui.route.login.v2

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.pickFile
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Path
import okio.buffer
import okio.use
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.PixivVerification
import top.kagg886.pixko.TokenType
import top.kagg886.pixko.module.user.getCurrentUserSimpleProfile
import top.kagg886.pmf.Res
import top.kagg886.pmf.account_verification_failed
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.backend.useTempFile
import top.kagg886.pmf.checking_token
import top.kagg886.pmf.no_file_selected
import top.kagg886.pmf.parsing_user_info
import top.kagg886.pmf.token_verification_failed
import top.kagg886.pmf.ui.route.login.v2.LoginType.BrowserLogin
import top.kagg886.pmf.ui.route.login.v2.LoginType.InputTokenLogin
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.util.logger
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.welcome_user

class LoginScreenViewModel :
    ContainerHost<LoginViewState, LoginSideEffect>,
    ViewModel(),
    ScreenModel,
    KoinComponent {
    private val storage by inject<PixivTokenStorage>()
    override val container: Container<LoginViewState, LoginSideEffect> =
        container(LoginViewState.WaitChooseLogin)

    @OptIn(OrbitExperimental::class)
    fun selectLoginType(loginType: LoginType) = intent {
        runOn<LoginViewState.LoginType.BrowserLogin.Error> {
            reduce {
                LoginViewState.WaitChooseLogin
            }
        }
        runOn<LoginViewState.WaitChooseLogin> {
            when (loginType) {
                InputTokenLogin -> reduce {
                    LoginViewState.LoginType.InputTokenLogin
                }

                BrowserLogin -> {
                    if (currentPlatform is Platform.Desktop) {
                        initKCEF().join()
                        return@runOn
                    }
                    reduce {
                        LoginViewState.LoginType.BrowserLogin.ShowBrowser
                    }
                }
            }
        }
    }

    fun challengeRefreshToken(token: String) = intent {
        val checkingToken = getString(Res.string.checking_token)
        reduce {
            LoginViewState.ProcessingUserData(checkingToken)
        }
        val tempStorage = PixivTokenStorage().apply {
            this.setToken(TokenType.REFRESH, token)
        }

        val account = PixivConfig.newAccountFromConfig(tempStorage)

        val u = try {
            account.getCurrentUserSimpleProfile()
        } catch (e: Exception) {
            logger.e(e) { "check pixiv token status failed: ${e.message}" }
            null
        }
        if (u == null) {
            postSideEffect(LoginSideEffect.Toast(getString(Res.string.token_verification_failed)))
            reduce {
                LoginViewState.LoginType.InputTokenLogin
            }
            return@intent
        }
        PixivConfig.pixiv_user = u
        storage.setToken(TokenType.ACCESS, tempStorage.getToken(TokenType.ACCESS)!!)
        storage.setToken(TokenType.REFRESH, tempStorage.getToken(TokenType.REFRESH)!!)
        val welcomeString = getString(Res.string.welcome_user, u.name)

        reduce {
            LoginViewState.ProcessingUserData(welcomeString)
        }
        delay(3.seconds)
        postSideEffect(LoginSideEffect.NavigateToMain)
    }

    fun challengePixivLoginUrl(factory: PixivVerification<*>, url: String) = intent {
        val parsingUserInfo = getString(Res.string.parsing_user_info)
        reduce { LoginViewState.ProcessingUserData(parsingUserInfo) }
        val u = try {
            val account = factory.verify(url) {
                this.storage = this@LoginScreenViewModel.storage
            }
            account.getCurrentUserSimpleProfile()
        } catch (e: Exception) {
            logger.e(e) { "verify pixiv url failed: ${e.message}" }
            null
        }

        if (u == null) {
            postSideEffect(LoginSideEffect.Toast(getString(Res.string.account_verification_failed)))
            reduce {
                LoginViewState.LoginType.BrowserLogin.ShowBrowser
            }
            return@intent
        }
        PixivConfig.pixiv_user = u
        val welcomeString = getString(Res.string.welcome_user, u.name)

        reduce {
            LoginViewState.ProcessingUserData(welcomeString)
        }
        delay(3.seconds)
        postSideEffect(LoginSideEffect.NavigateToMain)
    }

    fun installKCEFLocal() = intent {
        val platformFile = FileKit.pickFile(
            type = PickerType.File(listOf("tar.gz")),
        )
        if (platformFile == null) {
            postSideEffect(LoginSideEffect.Toast(getString(Res.string.no_file_selected)))
            return@intent
        }
        useTempFile { tmp ->
            tmp.sink().buffer().use {
                val stream = platformFile.getStream()
                val buffer = ByteArray(2048)
                var len: Int
                while (stream.hasBytesAvailable()) {
                    len = stream.readInto(buffer, buffer.size)
                    it.write(buffer, 0, len)
                }

                it.flush()
            }
            initKCEFLocal(tmp).join()
        }
    }
}

enum class LoginType {
    InputTokenLogin,
    BrowserLogin,
}

expect fun LoginScreenViewModel.initKCEF(): Job
expect fun LoginScreenViewModel.initKCEFLocal(file: Path): Job

sealed interface LoginViewState {
    data object WaitChooseLogin : LoginViewState

    sealed interface LoginType : LoginViewState {

        data object InputTokenLogin : LoginType

        sealed interface BrowserLogin : LoginType {
            data class Loading(val msg: MutableStateFlow<String>) : BrowserLogin
            data object ShowBrowser : BrowserLogin
            data class Error(val exception: Throwable) : BrowserLogin
        }
    }

    data class ProcessingUserData(val msg: String) : LoginViewState
}

sealed interface LoginSideEffect {
    data object NavigateToMain : LoginSideEffect
    data class Toast(val msg: String) : LoginSideEffect
}
