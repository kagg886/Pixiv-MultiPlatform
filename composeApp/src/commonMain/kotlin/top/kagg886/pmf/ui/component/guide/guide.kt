package top.kagg886.pmf.ui.component.guide

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// thanks to StageGuard(from Animeko)
// link: https://www.figma.com/design/LET1n9mmDa6npDTIlUuJjU/Animeko?node-id=349-9250&t=hBPSAEVlsmuEWPJt-0
@Composable
fun GuideScaffold(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    skipButton: (@Composable () -> Unit)? = null,
    backButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            Spacer(Modifier.height(TopAppBarDefaults.MediumAppBarCollapsedHeight))
        },
        bottomBar = {
            Column {
                HorizontalDivider()
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        Modifier.padding(start = 32.dp),
                    ) {
                        backButton?.invoke()
                    }

                    Row(
                        Modifier.weight(1f).padding(end = 32.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Box(
                            Modifier.padding(horizontal = 8.dp),
                        ) {
                            skipButton?.invoke()
                        }
                        confirmButton.invoke()
                    }
                }
            }
        },
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(it).padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(bottom = 16.dp)) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.titleMedium.copy(color = ButtonDefaults.textButtonColors().contentColor),
                ) {
                    subTitle()
                }
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.headlineMedium.copy(lineHeight = 36.sp),
                ) {
                    title()
                }
            }

            content(this)
        }
    }
}
