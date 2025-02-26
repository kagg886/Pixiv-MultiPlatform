package top.kagg886.pmf.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 将support-item放置在ListItem下方的DSL。
 */
@Composable
fun SupportListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
) {
    Column(modifier) {
        ListItem(
            headlineContent = headlineContent,
            overlineContent = overlineContent,
            supportingContent = {},
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            colors = colors,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            modifier = Modifier.fillMaxWidth(),
        )
        supportingContent?.let {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = ListItemDefaults.colors().supportingTextColor)) {
                    it()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
