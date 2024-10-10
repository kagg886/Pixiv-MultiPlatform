package top.kagg886.pmf.ui.util

import androidx.lifecycle.ViewModel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pmf.util.CloudFlareDoH
import top.kagg886.pmf.util.ignoreSSL
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pmf.BuildConfig

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
)

class UpdateCheckViewModel : ContainerHost<UpdateCheckState, UpdateCheckSideEffect>, ViewModel() {
    override val container: Container<UpdateCheckState, UpdateCheckSideEffect> = container(UpdateCheckState.Loading) {
        checkUpdate()
    }

    val net = OkHttpClient.Builder().apply {
        ignoreSSL()
        dns(CloudFlareDoH)
    }.build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @OptIn(OrbitExperimental::class)
    fun dismiss() = intent {
        runOn<UpdateCheckState.HaveUpdate> {
            reduce {
                state.copy(dismiss = true)
            }
        }
    }

    fun checkUpdate() = intent {
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
            postSideEffect(UpdateCheckSideEffect.Toast("更新检测失败..."))
            return@intent
        }
        val data = result.getOrThrow()

        if (BuildConfig.APP_VERSION_NAME != data.versionName) {
            reduce {
                UpdateCheckState.HaveUpdate(data)
            }
            return@intent
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
