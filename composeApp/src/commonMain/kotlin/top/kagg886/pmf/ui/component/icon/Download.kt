package top.kagg886.pmf.ui.component.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Download: ImageVector = Builder(
    name = "Download",
    defaultWidth = 24.0.dp,
    defaultHeight = 24.0.dp,
    viewportWidth = 24.0f,
    viewportHeight = 24.0f,
).apply {
    path(
        fill = SolidColor(Color(0xFF5f6368)),
        stroke = null,
        strokeLineWidth = 0.0f,
        strokeLineCap = Butt,
        strokeLineJoin = Miter,
        strokeLineMiter = 4.0f,
        pathFillType = NonZero,
    ) {
        moveTo(5.0f, 20.0f)
        horizontalLineToRelative(14.0f)
        verticalLineToRelative(-2.0f)
        horizontalLineTo(5.0f)
        verticalLineTo(20.0f)
        close()
        moveTo(19.0f, 9.0f)
        horizontalLineToRelative(-4.0f)
        verticalLineTo(3.0f)
        horizontalLineTo(9.0f)
        verticalLineToRelative(6.0f)
        horizontalLineTo(5.0f)
        lineToRelative(7.0f, 7.0f)
        lineTo(19.0f, 9.0f)
        close()
    }
}.build()
