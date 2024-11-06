package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.NovelImagesSize
import top.kagg886.pixko.module.novel.getNovelContent
import top.kagg886.pixko.module.novel.getNovelDetail
import top.kagg886.pixko.module.novel.parser.NovelContentBlockType.*
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.NovelHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.container

class NovelDetailViewModel(val id: Long) : ViewModel(), ScreenModel,
    ContainerHost<NovelDetailViewState, NovelDetailSideEffect>, KoinComponent {
    override val container: Container<NovelDetailViewState, NovelDetailSideEffect> =
        container(NovelDetailViewState.Loading) {
            loadByNovelId(id)
        }
    private val client = PixivConfig.newAccountFromConfig()

    private val database by inject<AppDatabase>()

    fun loadByNovelId(id: Long) = intent {
        reduce {
            NovelDetailViewState.Loading
        }
        val result = kotlin.runCatching {
            client.getNovelDetail(id) to client.getNovelContent(id)
        }
        if (result.isFailure) {
            reduce { NovelDetailViewState.Error }
            return@intent
        }
        val (detail, content) = result.getOrThrow()
        val images = kotlin.runCatching { content.images }.getOrElse { emptyMap() }

        val nodeMap = linkedMapOf<Int, String?>()

        //异步获取image
        coroutineScope {
            for ((index, i) in content.data.withIndex()) {
                when (i.novelContentBlockType) {
                    PLAIN -> {
                        nodeMap[index] = i.value
                    }

                    JUMP_URI -> {
                        nodeMap[index] = "[${i.value}](${i.metadata})"
                    }

                    NOTATION -> {
                        nodeMap[index] = i.value
                    }

                    UPLOAD_IMAGE -> {
                        nodeMap[index] = "\n![上传图片](${images[i.value]!![NovelImagesSize.N480Mw]})\n"
                    }

                    PIXIV_IMAGE -> {
                        launch {
                            val url =
                                client.getIllustDetail(i.value!!.toLong()).contentImages[IllustImagesType.MEDIUM]?.get(0)!!
                            nodeMap[index] = "\n![${i.value}](${url})\n"
                        }
                    }

                    NEW_PAGE -> {
//                    nodeMap[index] = "\n***\n# Page ${pageIndex + 1}\n"
//                    pageIndex++
                    }

                    TITLE -> {
                        nodeMap[index] = "\n#### ${i.value}\n"
                    }

                    JUMP_PAGE -> {
//                    nodeMap[index] = "[跳转到第${i.value}页](#Page ${i.value})"
                    }
                }
            }
        }
        reduce { NovelDetailViewState.Success(detail, nodeMap.toSortedMap().values.joinToString("") { it ?: "" }) }
        if (AppConfig.recordNovelHistory) {
            database.novelHistoryDAO().insert(NovelHistory(id, detail, System.currentTimeMillis()))
        }
    }
}

sealed class NovelDetailViewState {
    data object Loading : NovelDetailViewState()
    data object Error : NovelDetailViewState()
    data class Success(val novel: Novel, val content: String) : NovelDetailViewState()
}

sealed class NovelDetailSideEffect {
    data class Toast(val msg: String) : NovelDetailSideEffect()
}