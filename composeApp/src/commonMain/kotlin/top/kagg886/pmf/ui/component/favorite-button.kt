package top.kagg886.pmf.ui.component

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

sealed class FavoriteState {
    data object Favorite : FavoriteState()
    data object Loading : FavoriteState()
    data object NotFavorite : FavoriteState()
}

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
    onModify: suspend (target: FavoriteState) -> Unit
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
                IconButton(
                    onClick = {
                        loading = true
                        scope.launch {
                            onModify(FavoriteState.Favorite)
                        }.invokeOnCompletion {
                            loading = false
                        }
                    }
                ) {
                    nonFavoriteIcon()
                }
            }
        }
    }
}