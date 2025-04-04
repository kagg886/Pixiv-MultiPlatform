package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import com.github.panpf.sketch.fetch.newBase64Uri
import com.github.panpf.sketch.fetch.newFileUri
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import kotlinx.datetime.Clock
import moe.tarsin.gif.Frame
import moe.tarsin.gif.GifEncodeRequest
import moe.tarsin.gif.encodeGifPlatform
import okio.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.*
import top.kagg886.pixko.module.ugoira.UgoiraFrame
import top.kagg886.pixko.module.ugoira.getUgoiraMetadata
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.unFollowUser
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.IllustHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.useTempDir
import top.kagg886.pmf.backend.useTempFile
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.util.exists
import top.kagg886.pmf.util.source
import top.kagg886.pmf.util.unzip
import top.kagg886.pmf.util.writeBytes

class IllustDetailViewModel(private val illust: Illust) :
    ContainerHost<IllustDetailViewState, IllustDetailSideEffect>,
    ViewModel(),
    ScreenModel,
    KoinComponent {
    override val container: Container<IllustDetailViewState, IllustDetailSideEffect> =
        container(IllustDetailViewState.Loading()) {
            load()
        }

    private val client = PixivConfig.newAccountFromConfig()
    private val net by inject<HttpClient>()

    fun load(showLoading: Boolean = true) = intent {
        val loadingState = IllustDetailViewState.Loading()
        if (showLoading) {
            reduce {
                loadingState
            }
        }

        if (illust.isUgoira && AppConfig.gifSupport) {
            val gif = cachePath.resolve("${illust.id}.gif")
            if (!gif.exists()) {
                loadingState.data.tryEmit("获取动图元数据")
                val meta = client.getUgoiraMetadata(illust)
                useTempFile { zip ->
                    loadingState.data.tryEmit("下载动图帧数据")
                    zip.writeBytes(net.get(meta.url.content).bodyAsBytes())

                    useTempDir { workDir ->
                        loadingState.data.tryEmit("解压动图帧数据至临时工作区")
                        zip.unzip(workDir)
                        val frames = meta.frames.map { (file, delay) ->
                            UgoiraFrame("$workDir/$file", delay)
                        }
                        loadingState.data.tryEmit("重新编码为GIF中")
                        encodeGifPlatform(GifEncodeRequest(frames.map { (a, b) -> Frame(a, b) }, 15, "$gif"))
                    }
                }
            }

            reduce {
                val uri = when (currentPlatform) {
                    // https://github.com/panpf/sketch/issues/239
                    Platform.Desktop.Windows -> gif.source().use { newBase64Uri("image/gif", it.buffer().readByteString().base64()) }
                    else -> newFileUri(gif)
                }
                IllustDetailViewState.Success.GIF(illust, uri)
            }
            saveDataBase(illust)
            return@intent
        }

        reduce {
            IllustDetailViewState.Success.Normal(illust)
        }
        // 部分API返回信息不全，需要重新拉取
        intent a@{
            val result = kotlin.runCatching {
                client.getIllustDetail(illust.id.toLong())
            }
            if (result.isFailure) {
                postSideEffect(IllustDetailSideEffect.Toast("获取原图信息失败~"))
                return@a
            }
            val i = result.getOrThrow()
            if (i.contentImages[IllustImagesType.ORIGIN] == null) {
                postSideEffect(IllustDetailSideEffect.Toast("无法获取原图~不知道是怎么回事捏~"))
            }
            saveDataBase(i)
            reduce {
                IllustDetailViewState.Success.Normal(i)
            }
        }
    }

//    fun loadByIllustId(id: Long, silent: Boolean = true) = intent {
//        if (silent) {
//            reduce { IllustDetailViewState.Loading }
//        }
//        val illust = kotlin.runCatching {
//            client.getIllustDetail(id)
//        }
//        if (illust.isFailure) {
//            if (silent) {
//                reduce { IllustDetailViewState.Error }
//            }
//            return@intent
//        }
//        loadByIllustBean(illust.getOrThrow())
//    }
//
//    fun loadByIllustBean(illust: Illust) = intent {
//        reduce {
//            IllustDetailViewState.Loading
//        }
//        reduce { IllustDetailViewState.Success(illust) }
//        saveDataBase()
//    }

    private val database by inject<AppDatabase>()

    private fun saveDataBase(i: Illust) = intent {
        if (!AppConfig.recordIllustHistory) {
            return@intent
        }
        database.illustHistoryDAO().insert(
            IllustHistory(
                id = i.id,
                illust = i,
                createTime = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    @OptIn(OrbitExperimental::class)
    fun likeIllust(
        visibility: BookmarkVisibility = BookmarkVisibility.PUBLIC,
        tags: List<Tag>? = null,
    ) = intent {
        runOn<IllustDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.bookmarkIllust(state.illust.id.toLong()) {
                    this.visibility = visibility
                    this.tags = tags
                }
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(IllustDetailSideEffect.Toast("收藏失败~"))
                return@runOn
            }

            reduce {
                val illust = state.illust.copy(isBookMarked = true)
                when (val s = state) {
                    is IllustDetailViewState.Success.Normal -> s.copy(illust = illust)
                    is IllustDetailViewState.Success.GIF -> s.copy(illust = illust)
                }
            }
            postSideEffect(IllustDetailSideEffect.Toast("收藏成功~"))
        }
    }

    @OptIn(OrbitExperimental::class)
    fun disLikeIllust() = intent {
        runOn<IllustDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.deleteBookmarkIllust(state.illust.id.toLong())
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(IllustDetailSideEffect.Toast("取消收藏失败~"))
                return@runOn
            }
            reduce {
                val illust = state.illust.copy(isBookMarked = false)
                when (val s = state) {
                    is IllustDetailViewState.Success.Normal -> s.copy(illust = illust)
                    is IllustDetailViewState.Success.GIF -> s.copy(illust = illust)
                }
            }
            postSideEffect(IllustDetailSideEffect.Toast("取消收藏成功~"))
        }
    }

    @OptIn(OrbitExperimental::class)
    fun followUser(private: Boolean = false) = intent {
        runOn<IllustDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.followUser(
                    state.illust.user.id,
                    if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC,
                )
            }
            if (result.isFailure) {
                postSideEffect(IllustDetailSideEffect.Toast("关注失败~"))
                return@runOn
            }
            if (private) {
                postSideEffect(IllustDetailSideEffect.Toast("悄悄关注是不想让别人看到嘛⁄(⁄ ⁄•⁄ω⁄•⁄ ⁄)⁄"))
            } else {
                postSideEffect(IllustDetailSideEffect.Toast("关注成功~"))
            }
            reduce {
                val illust = with(state.illust) { copy(user = user.copy(isFollowed = true)) }
                when (val s = state) {
                    is IllustDetailViewState.Success.Normal -> s.copy(illust = illust)
                    is IllustDetailViewState.Success.GIF -> s.copy(illust = illust)
                }
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun unFollowUser() = intent {
        runOn<IllustDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.unFollowUser(state.illust.user.id)
            }
            if (result.isFailure) {
                postSideEffect(IllustDetailSideEffect.Toast("取关失败~(*^▽^*)"))
                return@runOn
            }
            postSideEffect(IllustDetailSideEffect.Toast("取关成功~o(╥﹏╥)o"))
            reduce {
                val illust = with(state.illust) { copy(user = user.copy(isFollowed = true)) }
                when (val s = state) {
                    is IllustDetailViewState.Success.Normal -> s.copy(illust = illust)
                    is IllustDetailViewState.Success.GIF -> s.copy(illust = illust)
                }
            }
        }
    }

    fun clearStatus() = intent {
        reduce { IllustDetailViewState.Loading() }
    }
}

sealed class IllustDetailViewState {
    data class Loading(val data: MutableStateFlow<String> = MutableStateFlow("")) : IllustDetailViewState()
    data object Error : IllustDetailViewState()
    sealed class Success : IllustDetailViewState() {
        abstract val illust: Illust
        data class Normal(override val illust: Illust) : Success()
        data class GIF(override val illust: Illust, val data: String) : Success()
    }
}

sealed class IllustDetailSideEffect {
    data class Toast(val msg: String) : IllustDetailSideEffect()
}
