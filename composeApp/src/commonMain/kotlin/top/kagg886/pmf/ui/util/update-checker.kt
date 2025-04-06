package top.kagg886.pmf.ui.util

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.Settings
import com.russhwolf.settings.nullableString
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.PlatformEngine
import top.kagg886.pmf.backend.SystemConfig
import top.kagg886.pmf.update_check_failed
import top.kagg886.pmf.util.logger
import top.kagg886.pmf.version_latest
import top.kagg886.pmf.you_can_still_check_update_on_setting

@Serializable
data class Asset(
    val name: String,

    @SerialName("browser_download_url")
    val download: String,
)

@Serializable
data class Release(
    @SerialName("html_url")
    val url: String,
    @SerialName("tag_name")
    val tagName: String,
    val name: String,
    val assets: List<Asset>,
    val body: String,
)

class UpdateCheckViewModel(
    config: Settings = SystemConfig.getConfig(),
) : ContainerHost<UpdateCheckState, UpdateCheckSideEffect>, ViewModel() {
    override val container: Container<UpdateCheckState, UpdateCheckSideEffect> = container(UpdateCheckState.Loading) {
        if (AppConfig.checkUpdateOnStart) {
            checkUpdate()
        }
    }

    private var skipVersion by config.nullableString("skip_version")

    val net = HttpClient(PlatformEngine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
    }

    @OptIn(OrbitExperimental::class)
    fun dismiss() = intent {
        runOn<UpdateCheckState.HaveUpdate> {
            skipVersion = "v${BuildConfig.APP_VERSION_NAME}"
            postSideEffect(UpdateCheckSideEffect.Toast(getString(Res.string.you_can_still_check_update_on_setting)))
            reduce {
                state.copy(dismiss = true)
            }
        }
    }

    fun checkUpdate(force: Boolean = false) = intent {
        if (!force && skipVersion == "v${BuildConfig.APP_VERSION_NAME}") {
            if (AppConfig.checkSuccessToast) {
                postSideEffect(UpdateCheckSideEffect.Toast(getString(Res.string.version_latest)))
            }
            return@intent
        }
        val result = kotlin.runCatching {
            net.get("https://api.github.com/repos/kagg886/Pixiv-MultiPlatform/releases/latest").body<Release>()
        }
        if (result.isFailure) {
            result.exceptionOrNull()?.let {
                logger.e(it) { "update check failed: ${it.message}" }
            }
            if (AppConfig.checkFailedToast) {
                postSideEffect(UpdateCheckSideEffect.Toast(getString(Res.string.update_check_failed)))
            }
            return@intent
        }
        val data = result.getOrThrow()

        if ("v${BuildConfig.APP_VERSION_NAME}" != data.tagName) {
            reduce {
                UpdateCheckState.HaveUpdate(data)
            }
            return@intent
        }

        if (AppConfig.checkSuccessToast) {
            postSideEffect(UpdateCheckSideEffect.Toast(getString(Res.string.version_latest)))
        }

        reduce {
            UpdateCheckState.Loading
        }
    }
}

sealed class UpdateCheckState {
    data object Loading : UpdateCheckState()
    data class HaveUpdate(val release: Release, val dismiss: Boolean = false) : UpdateCheckState()
}

sealed class UpdateCheckSideEffect {
    data class Toast(val msg: String) : UpdateCheckSideEffect()
}
