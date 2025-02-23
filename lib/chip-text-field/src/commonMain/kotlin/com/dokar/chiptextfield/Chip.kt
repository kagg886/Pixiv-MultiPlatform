package com.dokar.chiptextfield

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Basic chip class
 */
@Stable
open class Chip(text: String) {
    internal var textFieldValue by mutableStateOf(TextFieldValue(text))

    internal val focus = FocusInteraction.Focus()

    val text: String get() = textFieldValue.text
}