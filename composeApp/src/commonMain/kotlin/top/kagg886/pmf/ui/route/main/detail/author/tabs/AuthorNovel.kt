package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pmf.ui.component.ErrorPage

@Composable
fun AuthorNovel(user: UserInfo) {
    ErrorPage(text = "等待更新哦~") {}
}