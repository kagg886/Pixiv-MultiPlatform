package top.kagg886.pmf.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class FavoriteState {
    data object Favorite : FavoriteState()
    data object Loading : FavoriteState()
    data object NotFavorite : FavoriteState()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteButton(
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    nonFavoriteIcon: @Composable () -> Unit = {
        Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = null)
    },
    favoriteIcon: @Composable () -> Unit = {
        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color.Red)
    },
    onDoubleClick: () -> Unit = {},
    onModify: suspend (target: FavoriteState) -> Unit,
) {
    var loading by remember { mutableStateOf(false) }
    val state by remember(isFavorite) {
        derivedStateOf {
            when {
                loading -> FavoriteState.Loading
                isFavorite -> FavoriteState.Favorite
                else -> FavoriteState.NotFavorite
            }
        }
    }
    val scope = rememberCoroutineScope()
    AnimatedContent(
        targetState = state,
        modifier = modifier.size(30.dp)
    ) {
        when (it) {
            FavoriteState.Favorite -> {
                IconButton(
                    onClick = {
                        loading = true
                        scope.launch {
                            onModify(FavoriteState.NotFavorite)
                        }.invokeOnCompletion {
                            loading = false
                        }
                    }
                ) {
                    favoriteIcon()
                }
            }

            FavoriteState.Loading -> {
                CircularProgressIndicator()
            }

            FavoriteState.NotFavorite -> {
                val interactionSource = remember { MutableInteractionSource() }
                val viewConfiguration = LocalViewConfiguration.current

                LaunchedEffect(interactionSource) {
                    var isLongClick = false

                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                onDoubleClick()
                            }

                            is PressInteraction.Release -> {
                                if (isLongClick.not()) {
                                    loading = true
                                    scope.launch {
                                        onModify(FavoriteState.Favorite)
                                    }.invokeOnCompletion {
                                        loading = false
                                    }
                                }
                            }
                        }
                    }
                }

                IconButton(
                    interactionSource = interactionSource,
                    onClick = {}
                ) {
                    nonFavoriteIcon()
                }
            }
        }
    }
}