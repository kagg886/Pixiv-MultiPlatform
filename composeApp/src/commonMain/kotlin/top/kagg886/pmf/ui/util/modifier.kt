package top.kagg886.pmf.ui.util

import androidx.compose.ui.Modifier
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun Modifier.ifThen(
    condition: Boolean,
    modifier: Modifier.Companion.() -> Modifier?,
): Modifier = if (condition) this.then(modifier(Modifier.Companion) ?: Modifier) else this
