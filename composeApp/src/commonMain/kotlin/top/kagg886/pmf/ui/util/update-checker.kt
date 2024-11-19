package top.kagg886.pmf.ui.util

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.Settings
import com.russhwolf.settings.string
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.SystemConfig
import top.kagg886.pmf.util.ignoreSSL

@Serializable
data class Asset(
    val name: String,

    @SerialName("browser_download_url")
    val download: String
)

@Serializable
data class Release(
    @SerialName("html_url")
    val url: String,
    @SerialName("tag_name")
    val versionCode: String,
    @SerialName("name")
    val versionName: String,
    val assets: List<Asset>,
    val body: String
)

class UpdateCheckViewModel(
    config: Settings = SystemConfig.getConfig()
) : ContainerHost<UpdateCheckState, UpdateCheckSideEffect>, ViewModel() {
    override val container: Container<UpdateCheckState, UpdateCheckSideEffect> = container(UpdateCheckState.Loading) {
        if (AppConfig.checkUpdateOnStart) {
            checkUpdate()
        }
    }

    private var latestVersion by config.string("latest_version", "v${BuildConfig.APP_VERSION_NAME}")

    val net = OkHttpClient.Builder().apply {
        ignoreSSL()
    }.build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @OptIn(OrbitExperimental::class)
    fun dismiss() = intent {
        runOn<UpdateCheckState.HaveUpdate> {
            latestVersion = state.release.versionName
            postSideEffect(UpdateCheckSideEffect.Toast("您仍可以前往设置中检查更新"))
            reduce {
                state.copy(dismiss = true)
            }
        }
    }

    fun checkUpdate(force:Boolean = false) = intent {
        val newVersion = if (force) "v${BuildConfig.APP_VERSION_NAME}" else latestVersion
        val result = kotlin.runCatching {
            json.decodeFromString<Release>(
                net.newCall(
                    Request.Builder()
                        .url("https://api.github.com/repos/kagg886/Pixiv-MultiPlatform/releases/latest")
                        .build()
                ).execute().body!!.string()
            )
        }
        if (result.isFailure) {
            if (AppConfig.checkFailedToast) {
                postSideEffect(UpdateCheckSideEffect.Toast("更新检测失败..."))
            }
            return@intent
        }
        val data = result.getOrThrow()

        if (newVersion != data.versionName) {
            reduce {
                UpdateCheckState.HaveUpdate(data)
            }
            return@intent
        }

        if (AppConfig.checkSuccessToast) {
            postSideEffect(UpdateCheckSideEffect.Toast("当前为最新版本"))
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
