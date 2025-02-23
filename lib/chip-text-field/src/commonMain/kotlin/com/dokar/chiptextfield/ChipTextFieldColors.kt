package com.dokar.chiptextfield

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

interface ChipTextFieldColors {
    @Composable
    fun textColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
    ): State<Color>

    @Composable
    fun cursorColor(isError: Boolean): State<Color>

    @Composable
    fun backgroundColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color>
}

internal class DefaultChipTextFieldColors(
    private val textColor: Color,
    private val disabledTextColor: Color,
    private val errorTextColor: Color,
    private val cursorColor: Color,
    private val errorCursorColor: Color,
    private val backgroundColor: Color,
    private val disabledBackgroundColor: Color,
) : ChipTextFieldColors {
    @Composable
    override fun textColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        return rememberUpdatedState(
            when {
                isError -> errorTextColor
                !enabled -> textColor
                else -> disabledTextColor
            }
        )
    }

    @Composable
    override fun cursorColor(isError: Boolean): State<Color> {
        return rememberUpdatedState(if (isError) cursorColor else errorCursorColor)
    }

    @Composable
    override fun backgroundColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        return rememberUpdatedState(if (enabled) backgroundColor else disabledBackgroundColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultChipTextFieldColors) return false

        if (textColor != other.textColor) return false
        if (disabledTextColor != other.disabledTextColor) return false
        if (errorTextColor != other.errorTextColor) return false
        if (cursorColor != other.cursorColor) return false
        if (errorCursorColor != other.errorCursorColor) return false
        if (backgroundColor != other.backgroundColor) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = textColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + errorTextColor.hashCode()
        result = 31 * result + cursorColor.hashCode()
        result = 31 * result + errorCursorColor.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        return result
    }
}