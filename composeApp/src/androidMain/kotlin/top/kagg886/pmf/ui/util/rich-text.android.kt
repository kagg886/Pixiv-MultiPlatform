package top.kagg886.pmf.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

@Composable
actual fun HTMLRichText(
    html: String,
    modifier: Modifier,
    color: Color,
    style: TextStyle
) {
    val scheme = MaterialTheme.colorScheme
    val dom = remember(html) {
        Jsoup.parse(html).body().childNodes()
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
                            node.text()
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
        modifier = modifier
    )
}