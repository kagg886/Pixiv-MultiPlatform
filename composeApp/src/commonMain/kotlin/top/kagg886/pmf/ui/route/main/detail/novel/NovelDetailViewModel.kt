package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
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

        var md = ""
        for (i in content.data) {
            when (i.novelContentBlockType) {
                PLAIN -> {
                    md += i.value
                }

                JUMP_URI -> {
                    md += "[${i.value}](${i.metadata})"
                }

                NOTATION -> {
                    md += i.value
                }

                UPLOAD_IMAGE -> {
                    md += "\n![上传图片](${images[i.value]!![NovelImagesSize.N480Mw]})\n"
                }

                PIXIV_IMAGE -> {
                    val url = client.getIllustDetail(i.value!!.toLong()).contentImages[IllustImagesType.MEDIUM]?.get(0)!!
                    md += "\n![${i.value}](${url})\n"
                }

                NEW_PAGE -> {
                    md += "\n***\n"
                }

                TITLE -> {
                    md += "\n# ${i.value}\n"
                }

                JUMP_PAGE -> {
                    md += "\n### 页码标记\n"
                }
            }
        }
        reduce { NovelDetailViewState.Success(detail, md) }
        database.novelHistoryDAO().insert(NovelHistory(id, detail, System.currentTimeMillis()))
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