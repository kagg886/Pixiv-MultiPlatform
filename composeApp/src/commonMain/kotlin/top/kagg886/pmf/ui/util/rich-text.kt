package top.kagg886.pmf.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import coil3.toUri
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.time.Clock
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.ui.component.ImagePreviewer
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen

sealed interface NovelNodeElement {
    data class Plain(val text: String) : NovelNodeElement
    data class JumpUri(val text: String, val uri: String) : NovelNodeElement
    data class Notation(val text: String, val notation: String) : NovelNodeElement
    data class UploadImage(val url: String, val size: androidx.compose.ui.geometry.Size) : NovelNodeElement
    data class PixivImage(val illust: Illust) : NovelNodeElement
    data class Title(val text: String) : NovelNodeElement
    data class NewPage(val index: Int) : NovelNodeElement
    data class JumpPage(val page: Int) : NovelNodeElement
}

fun AnnotatedString.Builder.withClickable(
    colors: ColorScheme,
    text: String,
    onClick: () -> Unit,
) {
    withLink(
        link = LinkAnnotation.Clickable(
            tag = Random(Clock.System.now().toEpochMilliseconds()).nextInt().toString(),
            styles = TextLinkStyles(
                hoveredStyle = SpanStyle(
                    color = colors.primaryContainer,
                    textDecoration = TextDecoration.Underline,
                ),
            ),
            linkInteractionListener = {
                onClick()
            },
        ),
    ) {
        this.append(text)
    }
}

fun AnnotatedString.Builder.withLink(
    colors: ColorScheme,
    link: String,
    display: String = link,
) {
    withLink(
        link = LinkAnnotation.Url(
            url = link,
            styles = TextLinkStyles(
                style = SpanStyle(color = colors.primary),
                hoveredStyle = SpanStyle(
                    color = colors.primaryContainer,
                    textDecoration = TextDecoration.Underline,
                ),
            ),
        ),
    ) {
        this.append(display)
    }
}

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
            data = previews.map(String::toUri),
            onDismiss = { previewIndex = -1 },
            startIndex = previewIndex,
        )
    }
    val textSize = remember {
        AppConfig.textSize.sp
    }
    val defaultTextStyle = LocalTextStyle.current

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
                            // iW   sW
                            // -- = --
                            // iH   ??
                            "pixiv_${i.illust.id}",
                            InlineTextContent(
                                Placeholder(
                                    screenWidth,
                                    with(density) { ((i.illust.height * (screenWidth * 0.85).toPx().absoluteValue / i.illust.width)).toSp() },
                                    PlaceholderVerticalAlign.Center,
                                ),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    val nav = LocalNavigator.currentOrThrow
                                    AsyncImage(
                                        model = i.illust.contentImages[IllustImagesType.LARGE, IllustImagesType.MEDIUM]?.get(0),
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                            .aspectRatio(i.illust.width.toFloat() / i.illust.height)
                                            .clickable { nav.push(IllustDetailScreen(i.illust)) },
                                        contentDescription = null,
                                    )
                                }
                            },
                        )
                    }

                    is NovelNodeElement.UploadImage -> {
                        put(
                            "upload_${i.url.hashCode()}",
                            InlineTextContent(
                                Placeholder(
                                    screenWidth,
                                    with(density) { ((i.size.height * (screenWidth * 0.85).toPx().absoluteValue / i.size.width)).toSp() },
                                    PlaceholderVerticalAlign.Center,
                                ),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    AsyncImage(
                                        model = i.url,
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .aspectRatio(i.size.width / i.size.height)
                                            .clickable { previewIndex = previews.indexOf(i.url) },
                                        contentDescription = null,
                                    )
                                }
                            },
                        )
                    }

                    is NovelNodeElement.NewPage -> {
                        put(
                            "page_${i.index}",
                            InlineTextContent(Placeholder(screenWidth, 2.sp, PlaceholderVerticalAlign.Top)) {
                                HorizontalDivider(Modifier.fillMaxWidth())
                            },
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
                            colors = colors,
                            link = i.uri,
                            display = i.text,
                        )
                    }

                    is NovelNodeElement.NewPage -> {
                        withStyle(ParagraphStyle(lineHeight = screenWidth, textIndent = TextIndent(firstLine = 0.sp))) {
                            appendInlineContent("page_${i.index}")
                        }
                    }

                    is NovelNodeElement.Notation -> {
                        append(i.notation)
                    }

                    is NovelNodeElement.UploadImage -> {
                        withStyle(ParagraphStyle(lineHeight = screenWidth, textIndent = TextIndent(firstLine = 0.sp))) {
                            appendInlineContent("upload_${i.url.hashCode()}")
                        }
                    }

                    is NovelNodeElement.PixivImage -> {
                        withStyle(ParagraphStyle(lineHeight = screenWidth, textIndent = TextIndent(firstLine = 0.sp))) {
                            appendInlineContent("pixiv_${i.illust.id}")
                        }
                    }

                    is NovelNodeElement.Plain -> {
                        fun String.replaceBigLines() = replace("(\\s*\\r?\\n){2,}\n".toRegex(), "\n")
                        if (AppConfig.autoTypo) {
                            with(i.text.replaceBigLines().lines()) {
                                appendLine(this[0])
                                drop(1).filter { it.isNotBlank() }.map {
                                    if (currentPlatform is Platform.Android) {
                                        return@map it.trim()
                                    }
                                    // 8个空格
                                    return@map "        ${it.trim()}"
                                }.forEach(this@buildAnnotatedString::appendLine)
                            }
                            continue
                        }
                        append(i.text)
                    }

                    is NovelNodeElement.Title -> {
                        appendLine()
                        withStyle(ParagraphStyle(textIndent = TextIndent(firstLine = 0.sp))) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = textSize * 1.5)) {
                                append(i.text)
                            }
                        }
                        appendLine()
                    }
                }
            }
        }
    }
    val style = remember {
        when {
            AppConfig.autoTypo -> TextStyle(
                textIndent = TextIndent(firstLine = textSize * 2),
                fontSize = textSize,
                lineHeight = 1.5.em,
            )

            else -> defaultTextStyle.copy(
                fontSize = textSize,
                lineHeight = 1.5.em,
            )
        }
    }
    Text(
        text = annotateString,
        inlineContent = inlineNode,
        fontSize = textSize,
        style = style,
        modifier = modifier.onGloballyPositioned {
            screenWidth = with(density) {
                val offset = it.positionInParent()
                (it.boundsInParent().width - offset.x).toSp()
            }
        },
    )
}

@Composable
fun HTMLRichText(
    html: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val scheme = MaterialTheme.colorScheme
    val dom = remember(html) {
        Ksoup.parse(html).body().childNodes()
    }

    fun AnnotatedString.Builder.appendHTMLNode(nodes: List<Node>) {
        for (node in nodes) {
            when (node) {
                is TextNode -> append(node.text())
                is Element -> {
                    when (node.tagName()) {
                        "strong" -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                if (node.childNodes().isNotEmpty()) {
                                    appendHTMLNode(node.childNodes())
                                    return@withStyle
                                }
                                append(node.text())
                            }
                        }

                        "br" -> appendLine()
                        "a" -> withLink(
                            scheme,
                            node.attr("href"),
                            node.text(),
                        )

                        else -> append(node.html())
                    }
                }

                else -> append(node.outerHtml())
            }
        }
    }
    Text(
        buildAnnotatedString {
            appendHTMLNode(dom)
        },
        style = style,
        color = color,
        modifier = modifier,
    )
}
