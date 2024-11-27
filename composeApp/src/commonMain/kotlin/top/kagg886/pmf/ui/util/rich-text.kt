package top.kagg886.pmf.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pmf.ui.component.ImagePreviewer
import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen

sealed interface NovelNodeElement {
    data class Plain(val text: String) : NovelNodeElement
    data class JumpUri(val text: String, val uri: String) : NovelNodeElement
    data class Notation(val text: String, val notation: String) : NovelNodeElement
    data class UploadImage(val url: String) : NovelNodeElement
    data class PixivImage(val illust: Illust, val url: String) : NovelNodeElement
    data class Title(val text: String) : NovelNodeElement
    data class NewPage(val index: Int) : NovelNodeElement
    data class JumpPage(val page: Int) : NovelNodeElement
}

private val IMAGE_WIDTH = 80.sp

@Composable
fun RichText(
    state: List<NovelNodeElement>,
    modifier: Modifier = Modifier,
) {

    val previews = remember {
        state.filterIsInstance<NovelNodeElement.UploadImage>().map { it.url }
    }
    var previewIndex by remember { mutableStateOf(-1) }
    if (previewIndex != -1) {
        ImagePreviewer(
            url = previews,
            onDismiss = { previewIndex = -1 },
            startIndex = previewIndex
        )
    }

    val density = LocalDensity.current
    var screenWidth by remember {
        mutableStateOf(0.sp)
    }
    val inlineNode = remember(state, screenWidth) {
        buildMap {
            for (i in state) {
                when (i) {
                    is NovelNodeElement.PixivImage -> {
                        put(
                            "pixiv_${i.illust.id}",
                            InlineTextContent(Placeholder(screenWidth, IMAGE_WIDTH, PlaceholderVerticalAlign.Center)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    val nav = LocalNavigator.currentOrThrow
                                    ProgressedAsyncImage(
                                        url = i.url,
                                        modifier = Modifier.fillMaxHeight().clickable {
                                            nav.push(IllustDetailScreen(i.illust))
                                        },
                                        contentScale = ContentScale.FillHeight
                                    )
                                }
                            })
                    }

                    is NovelNodeElement.UploadImage -> {
                        put(
                            "upload_${i.url.hashCode()}",
                            InlineTextContent(Placeholder(screenWidth, IMAGE_WIDTH, PlaceholderVerticalAlign.Center)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    ProgressedAsyncImage(
                                        url = i.url,
                                        modifier = Modifier.fillMaxHeight().clickable {
                                            previewIndex = previews.indexOf(i.url)
                                        },
                                        contentScale = ContentScale.FillHeight
                                    )
                                }
                            }
                        )
                    }

                    is NovelNodeElement.NewPage -> {
                        put(
                            "page_${i.index}",
                            InlineTextContent(Placeholder(screenWidth, 2.sp, PlaceholderVerticalAlign.Top)) {
                                HorizontalDivider(Modifier.fillMaxWidth())
                            }
                        )
                    }

                    else -> continue
                }
            }
        }
    }
    val colors = MaterialTheme.colorScheme
    val annotateString = remember {
        buildAnnotatedString {
            for (i in state) {
                when (i) {
                    is NovelNodeElement.JumpPage -> {}

                    is NovelNodeElement.JumpUri -> {
                        withLink(
                            link = LinkAnnotation.Url(
                                url = i.uri.trim(),
                                styles = TextLinkStyles(
                                    style = SpanStyle(color = colors.primary),
                                    hoveredStyle = SpanStyle(
                                        color = colors.primaryContainer,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                )
                            ),
                        ) {
                            this.append(i.text)
                        }
                    }

                    is NovelNodeElement.NewPage -> {
                        appendInlineContent("page_${i.index}")
                    }

                    is NovelNodeElement.Notation -> {
                        append(i.notation)
                    }

                    is NovelNodeElement.UploadImage -> {
                        withStyle(ParagraphStyle(lineHeight = IMAGE_WIDTH)) {
                            appendInlineContent("upload_${i.url.hashCode()}")
                        }
                    }

                    is NovelNodeElement.PixivImage -> {
                        withStyle(ParagraphStyle(lineHeight = IMAGE_WIDTH)) {
                            appendInlineContent("pixiv_${i.illust.id}")
                        }
                    }

                    is NovelNodeElement.Plain -> {
                        append(i.text)
                    }

                    is NovelNodeElement.Title -> {
                        appendLine()
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp)) {
                            append(i.text)
                        }
                        appendLine()
                    }
                }
            }
        }
    }
    Text(
        text = annotateString,
        inlineContent = inlineNode,
        modifier = modifier.onGloballyPositioned {
            screenWidth = with(density) {
                it.boundsInParent().width.toSp()
            }
        }
    )
}