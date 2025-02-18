package com.dokar.chiptextfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Return a new remembered [ChipTextFieldState]
 *
 * @param chips Default chips
 */
@Composable
fun <T : Chip> rememberChipTextFieldState(
    chips: List<T> = emptyList()
): ChipTextFieldState<T> {
    return remember { ChipTextFieldState(chips = chips) }
}

/**
 * State class for [BasicChipTextField]
 *
 * @param chips Default chips
 */
@Stable
class ChipTextFieldState<T : Chip>(
    chips: List<T> = emptyList()
) {
    internal var disposed = false

    internal var defaultChips: List<T> = chips

    private var _focusedChip: T? by mutableStateOf(null)
    internal val focusedChip get() = _focusedChip

    private var _focusedChipIndex by mutableIntStateOf(-1)
    val focusedChipIndex get() = _focusedChipIndex

    internal var nextFocusedChipIndex by mutableIntStateOf(-1)

    internal var recordFocusedChip = true

    internal var textFieldFocusState by mutableStateOf(TextFieldFocusState.None)

    val isTextFieldFocused get() = textFieldFocusState == TextFieldFocusState.Focused

    var chips by mutableStateOf(chips)

    /**
     * Add a chip
     */
    fun addChip(chip: T) {
        val list = chips.toMutableList()
        list.add(chip)
        chips = list
    }

    /**
     * Remove chip
     */
    fun removeChip(chip: T) {
        val list = chips.toMutableList()
        val index = list.indexOf(chip)
        if (index == -1) {
            return
        }

        val focusedChipIndex = list.indexOf(focusedChip)
        if (focusedChipIndex == list.lastIndex && focusedChipIndex > 0) {
            if (index == list.lastIndex) {
                // The chip to remove is also the last chip, change the focused chip to the
                // previous chip
                updateFocusedChip(list[index - 1])
            }
            // IME will lose focus if the focused chip is the last, we move the focus to the
            // previous composable to avoid IME flash
            recordFocusedChip = false
            nextFocusedChipIndex = focusedChipIndex - 1
        } else if (chip == focusedChip && index < list.lastIndex) {
            // We are removing the focusing chip, simply set the focused chip to the next one.
            updateFocusedChip(list[index + 1])
        }

        list.remove(chip)
        chips = list
    }

    internal fun removeLastChip() {
        val list = chips.subList(0, chips.size - 1)
        chips = list
    }

    internal fun updateFocusedChip(chip: T?) {
        if (chip != null) {
            textFieldFocusState = TextFieldFocusState.None
        }
        this._focusedChip = chip
        this._focusedChipIndex = chips.indexOf(chip)
    }

    /**
     * Focus a chip by index.
     */
    fun focusChip(index: Int) {
        if (chips.isEmpty()) return
        if (index < 0 || index > chips.lastIndex) return
        updateFocusedChip(chips[index])
    }

    /**
     * Clear focus from a focused chip by index.
     */
    fun clearChipFocus(index: Int) {
        if (chips.isEmpty()) return
        if (index < 0 || index > chips.lastIndex) return
        val target = chips[index]
        if (focusedChip == target) {
            updateFocusedChip(null)
        }
    }

    /**
     * Focus the text field at the end of chips.
     */
    fun focusTextField() {
        textFieldFocusState = TextFieldFocusState.Focused
    }

    /**
     * Clear focus from the focused text field at the end chips.
     */
    fun clearTextFieldFocus() {
        textFieldFocusState = TextFieldFocusState.Unfocused
    }
}

internal enum class TextFieldFocusState {
    None,
    Focused,
    Unfocused,
}