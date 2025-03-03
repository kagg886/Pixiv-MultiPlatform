package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.github.panpf.sketch.fetch.newBase64Uri
import com.github.panpf.sketch.util.Uri
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import kotlinx.datetime.Clock
import okio.*
import okio.Path.Companion.toPath
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.e404.skiko.gif.gif
import top.e404.skiko.gif.listener.GIFMakingStep
import top.kagg886.gif.ImageBitmapDelegate
import top.kagg886.gif.toImageBitmap
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.*
import top.kagg886.pixko.module.ugoira.getUgoiraMetadata
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.unFollowUser
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.IllustHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.useTempDir
import top.kagg886.pmf.backend.useTempFile
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.util.exists
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.source
import top.kagg886.pmf.util.writeBytes

class IllustDetailViewModel(private val illust: Illust) :
    ContainerHost<IllustDetailViewState, IllustDetailSideEffect>,
    ViewModel(), ScreenModel, KoinComponent {
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

        if (illust.isUgoira) {
            val gif = cachePath.resolve("${illust.id}.gif")
            if (!gif.exists()) {
                loadingState.data.tryEmit("获取动图元数据")
                val meta = client.getUgoiraMetadata(illust)
                useTempFile { path ->
                    loadingState.data.tryEmit("下载动图帧数据")
                    val zip = with(path) {
                        writeBytes(net.get(meta.url.content).bodyAsBytes())
                        FileSystem.SYSTEM.openZip(this)
                    }

                    val frames = meta.frames.map {
                        { ImageBitmapDelegate(zip.source(it.file.toPath()).toImageBitmap()) } to it.delay
                    }

                    val data = useTempDir {
                        gif(illust.width, illust.height) {
                            table(frames[0].first())
                            loop(0)
                            workDir(it)
                            frame(frames[0].first)
                            scope(screenModelScope + Dispatchers.Default)
                            progress {
                                when (it) {
                                    is GIFMakingStep.CompressImage -> loadingState.data.tryEmit("处理动图像素帧: ${it.done} / ${it.total}")
                                    is GIFMakingStep.WritingData ->  loadingState.data.tryEmit("写出动图: ${it.done} / ${it.total}")
                                }
                            }

                            for (i in 1 until frames.size) {
                                frame(frames[i].first) {
                                    duration = frames[i].second
                                }
                            }
                        }
                    }

                    gif.sink().buffer().use {
                        data.buildToSink(it)
                        it.flush()
                    }
                }
            }

            reduce {
                loadingState.data.tryEmit("编码gif中")
                gif.source().use {
                    IllustDetailViewState.Success.GIF(illust, newBase64Uri("image/gif",it.buffer().readByteString().base64()) )
                }
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
                createTime = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    @OptIn(OrbitExperimental::class)
    fun likeIllust(
        visibility: BookmarkVisibility = BookmarkVisibility.PUBLIC,
        tags: List<Tag>? = null
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
                when (val s = state) {
                    is IllustDetailViewState.Success.Normal -> {
                        s.copy(
                            illust = state.illust.copy(isBookMarked = true)
                        )
                    }

                    else -> TODO()
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
                when (val s = state) {
                    is IllustDetailViewState.Success.Normal -> {
                        s.copy(
                            illust = state.illust.copy(isBookMarked = false)
                        )
                    }

                    else -> TODO()
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
                    if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC
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
                when (val s = state) {
                    is IllustDetailViewState.Success.Normal -> {
                        s.copy(
                            illust = state.illust.copy(
                                user = state.illust.user.copy(
                                    isFollowed = true
                                )
                            )
                        )
                    }

                    else -> TODO()
                }
//                state.copy(
//                    illust = state.illust.copy(
//                        user = state.illust.user.copy(
//                            isFollowed = true
//                        )
//                    )
//                )
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
                when (val s = state) {
                    is IllustDetailViewState.Success.Normal -> {
                        s.copy(
                            illust = state.illust.copy(
                                user = state.illust.user.copy(
                                    isFollowed = false
                                )
                            )
                        )
                    }

                    else -> TODO()
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
