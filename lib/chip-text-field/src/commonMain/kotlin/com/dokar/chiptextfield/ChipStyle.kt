package com.dokar.chiptextfield

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Chip style
 */
@Stable
interface ChipStyle {
    @Composable
    fun shape(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Shape>

    @Composable
    fun borderWidth(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp>

    @Composable
    fun borderColor(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Color>

    @Composable
    fun textColor(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Color>

    @Composable
    fun cursorColor(): State<Color>

    @Composable
    fun backgroundColor(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Color>
}

@Immutable
class DefaultChipStyle(
    private val shape: Shape,
    private val cursorColor: Color,
    private val focusedBorderWidth: Dp,
    private val unfocusedBorderWidth: Dp,
    private val disabledBorderWidth: Dp,
    private val focusedBorderColor: Color,
    private val unfocusedBorderColor: Color,
    private val disabledBorderColor: Color,
    private val focusedTextColor: Color,
    private val unfocusedTextColor: Color,
    private val disabledTextColor: Color,
    private val focusedBackgroundColor: Color,
    private val unfocusedBackgroundColor: Color,
    private val disabledBackgroundColor: Color,
) : ChipStyle {
    @Composable
    override fun shape(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Shape> {
        return rememberUpdatedState(shape)
    }

    @Composable
    override fun borderWidth(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        val focused by interactionSource.collectIsFocusedAsState()
        return rememberUpdatedState(
            when {
                !enabled -> disabledBorderWidth
                focused -> focusedBorderWidth
                else -> unfocusedBorderWidth
            }
        )
    }

    @Composable
    override fun borderColor(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()
        return rememberUpdatedState(
            when {
                !enabled -> disabledBorderColor
                focused -> focusedBorderColor
                else -> unfocusedBorderColor
            }
        )
    }

    @Composable
    override fun textColor(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()
        return rememberUpdatedState(
            when {
                !enabled -> disabledTextColor
                focused -> focusedTextColor
                else -> unfocusedTextColor
            }
        )
    }

    @Composable
    override fun cursorColor(): State<Color> {
        return rememberUpdatedState(cursorColor)
    }

    @Composable
    override fun backgroundColor(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()
        return rememberUpdatedState(
            when {
                !enabled -> disabledBackgroundColor
                focused -> focusedBackgroundColor
                else -> unfocusedBackgroundColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultChipStyle) return false

        if (shape != other.shape) return false
        if (cursorColor != other.cursorColor) return false
        if (focusedBorderWidth != other.focusedBorderWidth) return false
        if (unfocusedBorderWidth != other.unfocusedBorderWidth) return false
        if (disabledBorderWidth != other.disabledBorderWidth) return false
        if (focusedBorderColor != other.focusedBorderColor) return false
        if (unfocusedBorderColor != other.unfocusedBorderColor) return false
        if (disabledBorderColor != other.disabledBorderColor) return false
        if (focusedTextColor != other.focusedTextColor) return false
        if (unfocusedTextColor != other.unfocusedTextColor) return false
        if (disabledTextColor != other.disabledTextColor) return false
        if (focusedBackgroundColor != other.focusedBackgroundColor) return false
        if (unfocusedBackgroundColor != other.unfocusedBackgroundColor) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + cursorColor.hashCode()
        result = 31 * result + focusedBorderWidth.hashCode()
        result = 31 * result + unfocusedBorderWidth.hashCode()
        result = 31 * result + disabledBorderWidth.hashCode()
        result = 31 * result + focusedBorderColor.hashCode()
        result = 31 * result + unfocusedBorderColor.hashCode()
        result = 31 * result + disabledBorderColor.hashCode()
        result = 31 * result + focusedTextColor.hashCode()
        result = 31 * result + unfocusedTextColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + focusedBackgroundColor.hashCode()
        result = 31 * result + unfocusedBackgroundColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        return result
    }
}
