package top.kagg886.pmf.ui.util

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.ktor.http.Url
import top.kagg886.pmf.Res
import top.kagg886.pmf.jump_browser_tips
import top.kagg886.pmf.jump_browser_tips_question
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen
import top.kagg886.pmf.ui.route.main.detail.novel.NovelDetailScreen
import top.kagg886.pmf.util.stringResource
import top.kagg886.pmf.yes

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
                Text(stringResource(Res.string.jump_browser_tips))
            },
            text = {
                Text(stringResource(Res.string.jump_browser_tips_question, wantToOpenLink))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        origin.openUri(wantToOpenLink)
                    },
                ) {
                    Text(stringResource(Res.string.yes))
                }
            },
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
                        return@runCatching true
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
