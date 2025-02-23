package com.dokar.chiptextfield

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
fun <T : Chip> BasicCloseButton(
    state: ChipTextFieldState<T>,
    chip: T,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
    strokeColor: Color = Color.White,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 6.dp
) {
    Row(
        modifier = modifier
            .padding(start = startPadding, end = endPadding)
    ) {
        CloseButtonImpl(
            onClick = { state.removeChip(chip) },
            backgroundColor = backgroundColor,
            strokeColor = strokeColor
        )
    }
}

@Composable
private fun CloseButtonImpl(
    onClick: () -> Unit,
    backgroundColor: Color,
    strokeColor: Color,
    modifier: Modifier = Modifier,
) {
    val padding = with(LocalDensity.current) { 6.dp.toPx() }
    val strokeWidth = with(LocalDensity.current) { 1.2.dp.toPx() }
    val viewConfiguration = LocalViewConfiguration.current
    val viewConfigurationOverride = remember(viewConfiguration) {
        ViewConfigurationOverride(
            base = viewConfiguration,
            minimumTouchTargetSize = DpSize(24.dp, 24.dp)
        )
    }
    CompositionLocalProvider(LocalViewConfiguration provides viewConfigurationOverride) {
        Canvas(
            modifier = modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onClick)
        ) {
            drawLine(
                color = strokeColor,
                start = Offset(padding, padding),
                end = Offset(size.width - padding, size.height - padding),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = strokeColor,
                start = Offset(padding, size.height - padding),
                end = Offset(size.width - padding, padding),
                strokeWidth = strokeWidth
            )
        }
    }
}

internal class ViewConfigurationOverride(
    base: ViewConfiguration,
    override val doubleTapMinTimeMillis: Long = base.doubleTapMinTimeMillis,
    override val doubleTapTimeoutMillis: Long = base.doubleTapTimeoutMillis,
    override val longPressTimeoutMillis: Long = base.longPressTimeoutMillis,
    override val touchSlop: Float = base.touchSlop,
    override val minimumTouchTargetSize: DpSize = base.minimumTouchTargetSize
) : ViewConfiguration
