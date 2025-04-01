package top.kagg886.pmf.ui.component.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Robot by lazy {
    Builder(
        name = "Robot",
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
            moveTo(20.0f, 9.0f)
            verticalLineTo(7.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            horizontalLineToRelative(-3.0f)
            curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f)
            reflectiveCurveTo(9.0f, 3.34f, 9.0f, 5.0f)
            horizontalLineTo(6.0f)
            curveTo(4.9f, 5.0f, 4.0f, 5.9f, 4.0f, 7.0f)
            verticalLineToRelative(2.0f)
            curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
            curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
            verticalLineToRelative(4.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(12.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineToRelative(-4.0f)
            curveToRelative(1.66f, 0.0f, 3.0f, -1.34f, 3.0f, -3.0f)
            curveTo(23.0f, 10.34f, 21.66f, 9.0f, 20.0f, 9.0f)
            close()
            moveTo(7.5f, 11.5f)
            curveTo(7.5f, 10.67f, 8.17f, 10.0f, 9.0f, 10.0f)
            reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
            reflectiveCurveTo(9.83f, 13.0f, 9.0f, 13.0f)
            reflectiveCurveTo(7.5f, 12.33f, 7.5f, 11.5f)
            close()
            moveTo(16.0f, 17.0f)
            horizontalLineTo(8.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(17.0f)
            close()
            moveTo(15.0f, 13.0f)
            curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
            reflectiveCurveTo(14.17f, 10.0f, 15.0f, 10.0f)
            reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
            reflectiveCurveTo(15.83f, 13.0f, 15.0f, 13.0f)
            close()
        }
    }
        .build()
}
