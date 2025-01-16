package top.kagg886.pmf.ui.route.welcome

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import com.russhwolf.settings.Settings
import com.russhwolf.settings.boolean
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.SystemConfig
import top.kagg886.pmf.ui.util.container

class WelcomeModel(
    settings: Settings = SystemConfig.getConfig()
) : ContainerHost<WelcomeViewState, WelcomeSideEffect>, ViewModel(), ScreenModel {

    private var isInited by settings.boolean("welcome_init", false)

    override val container: Container<WelcomeViewState, WelcomeSideEffect> = container(WelcomeViewState.Loading) {
        if (isInited) {
            postSideEffect(WelcomeSideEffect.NavigateToMain)
            return@container
        }
        reduce {
            WelcomeViewState.ConfigureSetting.WELCOME
        }
    }

    @OptIn(OrbitExperimental::class)
    fun nextStep() = intent {
        runOn<WelcomeViewState.ConfigureSetting> {
            val nextState = WelcomeViewState.ConfigureSetting.entries.getOrNull(state.ordinal + 1)
            if (nextState == null) {
                isInited = true
                postSideEffect(WelcomeSideEffect.NavigateToMain)
                return@runOn
            }
            reduce { nextState }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun skipAll() = intent {
        runOn<WelcomeViewState.ConfigureSetting> {
            reduce { WelcomeViewState.ConfigureSetting.FINISH }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun goback() = intent {
        runOn<WelcomeViewState.ConfigureSetting> {
            val nextState = WelcomeViewState.ConfigureSetting.entries[state.ordinal - 1]
            reduce { nextState }
        }
    }
}

sealed interface WelcomeViewState {
    data object Loading : WelcomeViewState

    enum class ConfigureSetting : WelcomeViewState {
        WELCOME, //欢迎
        THEME, //配置主题
        BYPASS, //SNI绕过
        SHIELD, //屏蔽R18，AI等
        FINISH //完成
    }
}

sealed class WelcomeSideEffect {
    data object NavigateToMain : WelcomeSideEffect()
}