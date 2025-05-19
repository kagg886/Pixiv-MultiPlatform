package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import top.kagg886.pmf.Res
import top.kagg886.pmf.find_similar_illust
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.util.stringResource

class IllustSimilarScreen(val id:Long) : Screen {
    @Composable
    override fun Content() {
        val similarModel = rememberScreenModel("similar_illust_${id}") {
            IllustSimilarViewModel(id)
        }
        val nav = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(Res.string.find_similar_illust))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                nav.pop()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                )
            },
        ) {
            Box(Modifier.padding(it)) {
                IllustFetchScreen(similarModel)
            }
        }
    }
}