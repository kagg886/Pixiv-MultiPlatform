package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.kagg886.pixko.module.profile.CountryCode
import top.kagg886.pixko.module.user.UserInfo

@Composable
fun AuthorProfile(user: UserInfo) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        Title("账户信息")
        Value("用户名", user.user.name)
        Value("ID", user.user.id.toString())
        Value("PID", user.user.account)
        Title("个人资料")

        Value("生日", user.profile.birth?.toString() ?: "未知")
        Value("职业", user.profile.job.display)
        Value("地区", user.profile.region)
        Value(
            "地区",
            "${user.profile.country.display} ${if (user.profile.country === CountryCode.JAPAN) "---${user.profile.address}" else ""}"
        )
        Value("主页", user.profile.webpage ?: "未知")
        Value("twitter账号", user.profile.twitterAccount)
        Value("twitter链接", user.profile.twitterUrl ?: "未知")
        Value("pawoo链接", user.profile.pawooUrl ?: "未知")
        Value("是否为高级会员", if (user.profile.isPremium) "是" else "否")
    }
}

@Composable
private fun Title(text: String, modifier: Modifier = Modifier) {
    Item(modifier, trail = text)
}

@Composable
private fun Value(k: String, v: String, modifier: Modifier = Modifier) {
    Item(headline = k, supporting = v, modifier = modifier)
}

@Composable
private fun Item(
    modifier: Modifier = Modifier,
    headline: String? = null,
    supporting: String? = null,
    trail: String? = null,
) {
    ListItem(
        supportingContent = {
            if (supporting != null) {
                Text(supporting)
            }
        },
        overlineContent = {
            if (trail != null) {
                Text(trail)
            }
        },
        headlineContent = {
            if (headline != null) {
                Text(headline)
            }
        },
        modifier = modifier,
    )
}