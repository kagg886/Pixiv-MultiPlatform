package top.kagg886.pmf.ui.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import top.kagg886.pixko.User
import top.kagg886.pmf.res.*
import top.kagg886.pmf.ui.component.FavoriteButton
import top.kagg886.pmf.ui.component.FavoriteState
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.util.stringResource

@Composable
fun AuthorCard(
    modifier: Modifier = Modifier,
    user: User,
    followNumber: Int? = null,
    onCardClick: (nav: Navigator) -> Unit = { it.push(AuthorScreen(user.id)) },
    onFavoritePrivateClick: suspend () -> Unit = {},
    onFavoriteClick: suspend (Boolean) -> Unit = {},
) {
    val nav = LocalNavigator.currentOrThrow
    OutlinedCard(modifier = modifier, onClick = { onCardClick(nav) }) {
        ListItem(
            headlineContent = {
                Text(user.name)
            },
            supportingContent = {
                Text(user.comment?.lines()?.first()?.takeIf { it.isNotEmpty() } ?: stringResource(Res.string.no_description))
            },
            leadingContent = {
                AsyncImage(
                    model = user.profileImageUrls.content,
                    modifier = Modifier.size(35.dp).clip(CircleShape),
                    contentDescription = null,
                )
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FavoriteButton(
                        isFavorite = user.isFollowed == true,
                        onModify = {
                            onFavoriteClick(it == FavoriteState.Favorite)
                        },
                        onDoubleClick = {
                            onFavoritePrivateClick()
                        },
                    )
                    if (followNumber != null) {
                        // can't use rolling number here, because it can make some bug.
                        Text(followNumber.toString())
                    }
                }
            },
        )
    }
}
