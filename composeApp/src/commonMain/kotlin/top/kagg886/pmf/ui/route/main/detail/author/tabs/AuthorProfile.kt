package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.profile.CountryCode
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.res.*
import top.kagg886.pmf.ui.util.withClickable
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.util.setText
import top.kagg886.pmf.util.stringResource
@Composable
fun AuthorProfile(user: UserInfo) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scroll)) {
        val unknown = stringResource(Res.string.unknown)
        Title(stringResource(Res.string.account_info))
        Value(stringResource(Res.string.username), user.user.name)
        Value("ID", user.user.id.toString())
        Value("PID", user.user.account)
        Title(stringResource(Res.string.personal_profile))

        Value(stringResource(Res.string.birthday), user.profile.birth?.toString() ?: unknown)
        Value(stringResource(Res.string.job), user.profile.job.display)
        Value(stringResource(Res.string.region), user.profile.region)
        Value(
            stringResource(Res.string.country),
            "${user.profile.country.display} ${if (user.profile.country === CountryCode.JAPAN) "---${user.profile.address}" else ""}",
        )
        Value(stringResource(Res.string.homepage), user.profile.webpage ?: unknown)
        Value(stringResource(Res.string.twitter_account), user.profile.twitterAccount)
        Value(stringResource(Res.string.twitter_link), user.profile.twitterUrl ?: unknown)
        Value(stringResource(Res.string.pawoo_link), user.profile.pawooUrl ?: unknown)
        Value(stringResource(Res.string.is_premium_member), if (user.profile.isPremium) stringResource(Res.string.yes) else stringResource(Res.string.no))
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
                val clip = LocalClipboard.current
                val color = MaterialTheme.colorScheme
                val snack = LocalSnackBarHost.current
                val scope = rememberCoroutineScope()
                Text(
                    buildAnnotatedString {
                        withClickable(
                            colors = color,
                            text = supporting,
                            onClick = {
                                scope.launch {
                                    clip.setText(
                                        supporting,
                                    )
                                    snack.showSnackbar(getString(Res.string.copy_to_clipboard_success_args, headline ?: ""))
                                }
                            },
                        )
                    },
                )
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
