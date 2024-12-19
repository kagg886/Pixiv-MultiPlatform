package top.kagg886.pmf.ui.component.icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Copy: ImageVector by lazy {
	ImageVector.Builder(
		name = "File_copy_24dp_5F6368",
		defaultWidth = 24.dp,
		defaultHeight = 24.dp,
		viewportWidth = 24f,
		viewportHeight = 24f
	).apply {
		path(
			fill = null,
			fillAlpha = 1.0f,
			stroke = null,
			strokeAlpha = 1.0f,
			strokeLineWidth = 1.0f,
			strokeLineCap = StrokeCap.Butt,
			strokeLineJoin = StrokeJoin.Miter,
			strokeLineMiter = 1.0f,
			pathFillType = PathFillType.NonZero
		) {
			moveTo(0f, 0f)
			horizontalLineToRelative(24f)
			verticalLineToRelative(24f)
			horizontalLineTo(0f)
			close()
		}
		path(
			fill = SolidColor(Color(0xFF5F6368)),
			fillAlpha = 1.0f,
			stroke = null,
			strokeAlpha = 1.0f,
			strokeLineWidth = 1.0f,
			strokeLineCap = StrokeCap.Butt,
			strokeLineJoin = StrokeJoin.Miter,
			strokeLineMiter = 1.0f,
			pathFillType = PathFillType.NonZero
		) {
			moveTo(16f, 1f)
			horizontalLineTo(4f)
			curveToRelative(-1.10f, 00f, -20f, 0.90f, -20f, 20f)
			verticalLineToRelative(14f)
			horizontalLineToRelative(2f)
			verticalLineTo(3f)
			horizontalLineToRelative(12f)
			verticalLineTo(1f)
			close()
			moveToRelative(-1f, 4f)
			lineToRelative(6f, 6f)
			verticalLineToRelative(10f)
			curveToRelative(00f, 1.10f, -0.90f, 20f, -20f, 20f)
			horizontalLineTo(7.99f)
			curveTo(6.890f, 230f, 60f, 22.10f, 60f, 210f)
			lineToRelative(0.01f, -14f)
			curveToRelative(00f, -1.10f, 0.890f, -20f, 1.990f, -20f)
			horizontalLineToRelative(7f)
			close()
			moveToRelative(-1f, 7f)
			horizontalLineToRelative(5.5f)
			lineTo(14f, 6.5f)
			verticalLineTo(12f)
			close()
		}
	}.build()
}