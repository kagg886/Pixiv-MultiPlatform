package top.kagg886.pmf.ui.util

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.ktor.http.*
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen
import top.kagg886.pmf.ui.route.main.detail.novel.NovelDetailScreen

@Composable
fun rememberSupportPixivNavigateUriHandler(): UriHandler {
    val origin = LocalUriHandler.current
    var wantToOpenLink by remember {
        mutableStateOf("")
    }

    val showLink by remember {
        derivedStateOf {
            wantToOpenLink.isNotBlank()
        }
    }

    if (showLink) {
        AlertDialog(
            onDismissRequest = {
                wantToOpenLink = ""
            },
            title = {
                Text("外部链接确认提示")
            },
            text = {
                Text("是否打开链接：$wantToOpenLink")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        origin.openUri(wantToOpenLink)
                    }
                ) {
                    Text("确定")
                }
            }
        )
    }

    val nav = LocalNavigator.currentOrThrow
    return remember(origin) {
        object : UriHandler {
            override fun openUri(url: String) {
                val unit = kotlin.runCatching {
                    if (url.contains("pixiv.net")) {
                        val uri = Url(url.trim())
                        when {
                            uri.encodedPath.startsWith("/users/") -> nav.push(AuthorScreen(uri.encodedPath.split("/")[2].toInt()))
                            uri.encodedPath.startsWith("/novel/show.php") -> nav.push(NovelDetailScreen(uri.encodedPath.split("=")[1].toLong()))
                            uri.encodedPath.startsWith("/artworks/") -> nav.push(IllustDetailScreen.PreFetch(uri.encodedPath.split("/")[2].toLong()))
                        }
                        return@runCatching true
                    }
                    if (url.startsWith("pixiv://")) {
                        val uri = Url(url.trim())
                        when (uri.host) {
                            "novels" -> nav.push(AuthorScreen(uri.encodedPath.substring(1).toInt()))
                            "illusts" -> nav.push(IllustDetailScreen.PreFetch(uri.encodedPath.substring(1).toLong()))
                            "users" -> nav.push(AuthorScreen(uri.encodedPath.substring(1).toInt()))
                        }
                    }
                    false
                }
                if (unit.isFailure || !unit.getOrThrow()) {
                    wantToOpenLink = url.trim()
                }
            }
        }
    }
}