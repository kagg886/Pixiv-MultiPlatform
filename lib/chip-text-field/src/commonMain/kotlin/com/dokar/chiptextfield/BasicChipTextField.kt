package com.dokar.chiptextfield

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dokar.chiptextfield.util.StableHolder
import com.dokar.chiptextfield.util.filterNewLines
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * A text field can display chips, press enter to create a new chip.
 *
 * @param state Use [rememberChipTextFieldState] to create new state.
 * @param onSubmit Called after pressing enter key, used to create new chips.
 * @param modifier Modifier for chip text field.
 * @param enabled Enabled state, if false, user will not able to edit and select.
 * @param readOnly If true, edit will be disabled, but user can still select text.
 * @param readOnlyChips If true, chips are no more editable, but the text field can still be edited
 * if [readOnly] is not true.
 * @param isError Error state, it is used to change cursor color.
 * @param keyboardOptions See [BasicTextField] for the details.
 * @param textStyle Text style, also apply to text in chips.
 * @param chipStyle Chip style, include shape, text color, background color, etc. See [ChipStyle].
 * @param chipVerticalSpacing Vertical spacing between chips.
 * @param chipHorizontalSpacing Horizontal spacing between chips.
 * @param chipLeadingIcon Leading chip icon, nothing will be displayed by default.
 * @param chipTrailingIcon Trailing chip icon, by default, a [BasicCloseButton] will be displayed.
 * @param onChipClick Chip click action.
 * @param onChipLongClick Chip long click action.
 * @param colors Colors of the chip text field. Defaults to
 * [BasicChipTextFieldDefaults.chipTextFieldColors].
 * @param decorationBox The decoration box to wrap around text field.
 *
 * @see BasicTextField
 */
@Composable
fun <T : Chip> BasicChipTextField(
    state: ChipTextFieldState<T>,
    onSubmit: (value: String) -> T?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    readOnlyChips: Boolean = readOnly,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = BasicChipTextFieldDefaults.textStyle,
    chipStyle: ChipStyle = BasicChipTextFieldDefaults.chipStyle(),
    chipVerticalSpacing: Dp = 4.dp,
    chipHorizontalSpacing: Dp = 4.dp,
    chipLeadingIcon: @Composable (chip: T) -> Unit = {},
    chipTrailingIcon: @Composable (chip: T) -> Unit = { BasicCloseButton(state, it) },
    onChipClick: ((chip: T) -> Unit)? = null,
    onChipLongClick: ((chip: T) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ChipTextFieldColors = BasicChipTextFieldDefaults.chipTextFieldColors(),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    var value by remember { mutableStateOf(TextFieldValue()) }
    val onValueChange: (TextFieldValue) -> Unit = { value = it }
    BasicChipTextField(
        state = state,
        onSubmit = { onSubmit(it.text) },
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        readOnlyChips = readOnlyChips,
        isError = isError,
        keyboardOptions = keyboardOptions,
        textStyle = textStyle,
        chipStyle = chipStyle,
        chipVerticalSpacing = chipVerticalSpacing,
        chipHorizontalSpacing = chipHorizontalSpacing,
        chipLeadingIcon = chipLeadingIcon,
        chipTrailingIcon = chipTrailingIcon,
        onChipClick = onChipClick,
        onChipLongClick = onChipLongClick,
        interactionSource = interactionSource,
        colors = colors,
        decorationBox = decorationBox,
    )
}

/**
 * A text field can display chips, press enter to create a new chip.
 *
 * @param state Use [rememberChipTextFieldState] to create new state.
 * @param value The value of text field.
 * @param onValueChange Called when the value in ChipTextField has changed.
 * @param onSubmit Called after pressing enter key, used to create new chips.
 * @param modifier Modifier for chip text field.
 * @param enabled Enabled state, if false, user will not able to edit and select.
 * @param readOnly If true, edit will be disabled, but user can still select text.
 * @param readOnlyChips If true, chips are no more editable, but the text field can still be edited
 * if [readOnly] is not true.
 * @param isError Error state, it is used to change cursor color.
 * @param keyboardOptions See [BasicTextField] for the details.
 * @param textStyle Text style, also apply to text in chips.
 * @param chipStyle Chip style, include shape, text color, background color, etc. See [ChipStyle].
 * @param chipVerticalSpacing Vertical spacing between chips.
 * @param chipHorizontalSpacing Horizontal spacing between chips.
 * @param chipLeadingIcon Leading chip icon, nothing will be displayed by default.
 * @param chipTrailingIcon Trailing chip icon, by default, a [BasicCloseButton] will be displayed.
 * @param onChipClick Chip click action.
 * @param onChipLongClick Chip long click action.
 * @param colors Colors of the chip text field. Defaults to
 * [BasicChipTextFieldDefaults.chipTextFieldColors].
 * @param decorationBox The decoration box to wrap around text field.
 *
 * @see BasicTextField
 */
@Composable
fun <T : Chip> BasicChipTextField(
    state: ChipTextFieldState<T>,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (value: String) -> T?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    readOnlyChips: Boolean = readOnly,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = BasicChipTextFieldDefaults.textStyle,
    chipStyle: ChipStyle = BasicChipTextFieldDefaults.chipStyle(),
    chipVerticalSpacing: Dp = 4.dp,
    chipHorizontalSpacing: Dp = 4.dp,
    chipLeadingIcon: @Composable (chip: T) -> Unit = {},
    chipTrailingIcon: @Composable (chip: T) -> Unit = { BasicCloseButton(state, it) },
    onChipClick: ((chip: T) -> Unit)? = null,
    onChipLongClick: ((chip: T) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ChipTextFieldColors = BasicChipTextFieldDefaults.chipTextFieldColors(),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    // Copied from androidx.compose.foundation.text.BasicTextField.kt
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)
    SideEffect {
        if (textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }
    var lastTextValue by remember(value) { mutableStateOf(value) }
    val mappedOnValueChange: (TextFieldValue) -> Unit = { newTextFieldValueState ->
        textFieldValueState = newTextFieldValueState

        val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
        lastTextValue = newTextFieldValueState.text

        if (stringChangedSinceLastInvocation) {
            onValueChange(newTextFieldValueState.text)
        }
    }
    BasicChipTextField(
        state = state,
        onSubmit = { onSubmit(it.text) },
        value = textFieldValue,
        onValueChange = mappedOnValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        readOnlyChips = readOnlyChips,
        isError = isError,
        keyboardOptions = keyboardOptions,
        textStyle = textStyle,
        chipStyle = chipStyle,
        chipVerticalSpacing = chipVerticalSpacing,
        chipHorizontalSpacing = chipHorizontalSpacing,
        chipLeadingIcon = chipLeadingIcon,
        chipTrailingIcon = chipTrailingIcon,
        onChipClick = onChipClick,
        onChipLongClick = onChipLongClick,
        interactionSource = interactionSource,
        colors = colors,
        decorationBox = decorationBox,
    )
}

/**
 * A text field can display chips, press enter to create a new chip.
 *
 * @param state Use [rememberChipTextFieldState] to create new state.
 * @param value The value of text field.
 * @param onValueChange Called when the value in ChipTextField has changed.
 * @param onSubmit Called after pressing enter key, used to create new chips.
 * @param modifier Modifier for chip text field.
 * @param enabled Enabled state, if false, user will not able to edit and select.
 * @param readOnly If true, edit will be disabled, but user can still select text.
 * @param readOnlyChips If true, chips are no more editable, but the text field can still be edited
 * if [readOnly] is not true.
 * @param isError Error state, it is used to change cursor color.
 * @param keyboardOptions See [BasicTextField] for the details.
 * @param textStyle Text style, also apply to text in chips.
 * @param chipStyle Chip style, include shape, text color, background color, etc. See [ChipStyle].
 * @param chipVerticalSpacing Vertical spacing between chips.
 * @param chipHorizontalSpacing Horizontal spacing between chips.
 * @param chipLeadingIcon Leading chip icon, nothing will be displayed by default.
 * @param chipTrailingIcon Trailing chip icon, by default, a [BasicCloseButton] will be displayed.
 * @param onChipClick Chip click action.
 * @param onChipLongClick Chip long click action.
 * @param colors Colors of the chip text field. Defaults to
 * [BasicChipTextFieldDefaults.chipTextFieldColors].
 * @param decorationBox The decoration box to wrap around text field.
 *
 * @see BasicTextField
 */
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun <T : Chip> BasicChipTextField(
    state: ChipTextFieldState<T>,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: (value: TextFieldValue) -> T?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    readOnlyChips: Boolean = readOnly,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = BasicChipTextFieldDefaults.textStyle,
    chipStyle: ChipStyle = BasicChipTextFieldDefaults.chipStyle(),
    chipVerticalSpacing: Dp = 4.dp,
    chipHorizontalSpacing: Dp = 4.dp,
    chipLeadingIcon: @Composable (chip: T) -> Unit = {},
    chipTrailingIcon: @Composable (chip: T) -> Unit = { BasicCloseButton(state, it) },
    onChipClick: ((chip: T) -> Unit)? = null,
    onChipLongClick: ((chip: T) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ChipTextFieldColors = BasicChipTextFieldDefaults.chipTextFieldColors(),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    val scope = rememberCoroutineScope()

    val textFieldFocusRequester = remember { FocusRequester() }

    val editable = enabled && !readOnly

    val keyboardController = LocalSoftwareKeyboardController.current

    val bringLastIntoViewRequester = remember { StableHolder(BringIntoViewRequester()) }

    val hasFocusedChipBeforeEmpty = remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(state) {
        launch {
            snapshotFlow { state.focusedChip != null }
                .filter { state.chips.isNotEmpty() }
                .collect { hasFocusedChipBeforeEmpty.value = it }
        }
        snapshotFlow { state.chips }
            .filter { it.isEmpty() }
            .collect {
                if (hasFocusedChipBeforeEmpty.value) {
                    runCatching { textFieldFocusRequester.requestFocus() }
                }
                hasFocusedChipBeforeEmpty.value = false
            }
    }

    LaunchedEffect(state, state.disposed) {
        if (state.disposed) {
            state.chips = state.defaultChips
            state.disposed = false
        }
    }

    LaunchedEffect(focusManager, state, textFieldFocusRequester) {
        snapshotFlow { state.textFieldFocusState }
            .distinctUntilChanged()
            .collect {
                when (it) {
                    TextFieldFocusState.None -> {}
                    TextFieldFocusState.Focused -> runCatching {
                        textFieldFocusRequester.requestFocus()
                    }

                    TextFieldFocusState.Unfocused -> {
                        focusManager.clearFocus()
                        textFieldFocusRequester.freeFocus()
                    }
                }
            }
    }

    DisposableEffect(state) {
        onDispose {
            state.disposed = true
        }
    }

    decorationBox {
        FlowRow(
            modifier = modifier
                .pointerInput(value) {
                    detectTapGestures(
                        onTap = {
                            if (!editable) return@detectTapGestures
                            keyboardController?.show()
                            runCatching { textFieldFocusRequester.requestFocus() }
                            state.updateFocusedChip(null)
                            state.focusTextField()
                            // Move cursor to the end
                            val selection = value.text.length
                            onValueChange(value.copy(selection = TextRange(selection)))
                            scope.launch { bringLastIntoViewRequester.value.bringIntoView() }
                        },
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(chipHorizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(chipVerticalSpacing),
        ) {
            val focuses = remember { StableHolder(mutableSetOf<FocusInteraction.Focus>()) }
            Chips(
                state = state,
                enabled = enabled,
                readOnly = readOnly || readOnlyChips,
                onRemoveRequest = { state.removeChip(it) },
                onFocused = {
                    if (!focuses.value.contains(it)) {
                        focuses.value.add(it)
                        interactionSource.tryEmit(it)
                    }
                },
                onFreeFocus = {
                    focuses.value.remove(it)
                    interactionSource.tryEmit(FocusInteraction.Unfocus(it))
                },
                onLoseFocus = {
                    scope.launch {
                        awaitFrame()
                        runCatching { textFieldFocusRequester.requestFocus() }
                    }
                    state.updateFocusedChip(null)
                },
                onChipClick = onChipClick,
                onChipLongClick = onChipLongClick,
                textStyle = textStyle,
                chipStyle = chipStyle,
                chipLeadingIcon = chipLeadingIcon,
                chipTrailingIcon = chipTrailingIcon,
                bringLastIntoViewRequester = bringLastIntoViewRequester,
            )

            Input(
                state = state,
                onSubmit = {
                    val chip = onSubmit(it)
                    if (chip != null) {
                        scope.launch {
                            awaitFrame()
                            bringLastIntoViewRequester.value.bringIntoView()
                            state.focusTextField()
                        }
                    }
                    chip
                },
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                readOnly = readOnly,
                isError = isError,
                textStyle = textStyle,
                colors = colors,
                keyboardOptions = keyboardOptions,
                focusRequester = textFieldFocusRequester,
                interactionSource = interactionSource,
                onFocusChange = { isFocused ->
                    if (isFocused) {
                        state.updateFocusedChip(null)
                    }
                },
                modifier = Modifier
                    .padding(top = chipVerticalSpacing)
                    .bringIntoViewRequester(bringLastIntoViewRequester.value),
            )
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
private fun <T : Chip> Chips(
    state: ChipTextFieldState<T>,
    enabled: Boolean,
    readOnly: Boolean,
    onRemoveRequest: (T) -> Unit,
    onFocused: (FocusInteraction.Focus) -> Unit,
    onFreeFocus: (FocusInteraction.Focus) -> Unit,
    onLoseFocus: () -> Unit,
    onChipClick: ((chip: T) -> Unit)?,
    onChipLongClick: ((chip: T) -> Unit)?,
    textStyle: TextStyle,
    chipStyle: ChipStyle,
    chipLeadingIcon: @Composable (chip: T) -> Unit,
    chipTrailingIcon: @Composable (chip: T) -> Unit,
    bringLastIntoViewRequester: StableHolder<BringIntoViewRequester>,
) {
    val chips = state.chips

    val focusRequesters = remember(chips.size) {
        List(chips.size) { FocusRequester() }
    }

    fun focusChip(index: Int) {
        runCatching { focusRequesters[index].requestFocus() }
        val targetChip = chips[index]
        targetChip.textFieldValue = targetChip.textFieldValue.copy(
            selection = TextRange(targetChip.text.length),
        )
    }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(focusManager, focusRequesters, chips, state.focusedChip) {
        state.recordFocusedChip = true
        val chip = state.focusedChip
        if (chip == null) {
            focusRequesters.forEach { it.freeFocus() }
            if (!state.isTextFieldFocused) {
                focusManager.clearFocus()
            }
            return@LaunchedEffect
        }
        val index = chips.indexOf(chip)
        if (index == -1) return@LaunchedEffect
        runCatching { focusRequesters[index].requestFocus() }
        onFocused(chip.focus)
    }

    LaunchedEffect(chips) {
        snapshotFlow { state.nextFocusedChipIndex }
            .distinctUntilChanged()
            .filter { it != -1 }
            .collect { index ->
                if (index in 0..chips.lastIndex) {
                    runCatching { focusRequesters[index].requestFocus() }
                    onFocused(chips[index].focus)
                } else if (index != -1) {
                    if (chips.isNotEmpty()) {
                        runCatching { focusRequesters[chips.lastIndex].requestFocus() }
                    } else {
                        onLoseFocus()
                    }
                }
                state.nextFocusedChipIndex = -1
            }
    }

    for ((index, chip) in chips.withIndex()) {
        ChipItem(
            state = state,
            focusRequester = focusRequesters[index],
            chip = chip,
            enabled = enabled,
            readOnly = readOnly,
            onRemoveRequest = {
                // Call before removing chip
                onFreeFocus(chip.focus)
                if (chips.size > 1) {
                    focusChip((index - 1).coerceAtLeast(0))
                } else {
                    onLoseFocus()
                }
                onRemoveRequest(chip)
            },
            onFocusNextRequest = {
                onFreeFocus(chip.focus)
                if (index < chips.lastIndex) {
                    focusChip(index + 1)
                } else {
                    onLoseFocus()
                    focusRequesters[index].freeFocus()
                }
            },
            onFocusChange = { isFocused ->
                if (isFocused) {
                    if (state.recordFocusedChip) {
                        state.updateFocusedChip(chip)
                    }
                    onFocused(chip.focus)
                } else {
                    onFreeFocus(chip.focus)
                }
            },
            onClick = { onChipClick?.invoke(chip) },
            onLongClick = { onChipLongClick?.invoke(chip) },
            textStyle = textStyle,
            chipStyle = chipStyle,
            chipLeadingIcon = chipLeadingIcon,
            chipTrailingIcon = chipTrailingIcon,
            modifier = if (index == chips.lastIndex) {
                Modifier.bringIntoViewRequester(bringLastIntoViewRequester.value)
            } else {
                Modifier
            },
        )
    }
}

@ExperimentalComposeUiApi
@Composable
private fun <T : Chip> Input(
    state: ChipTextFieldState<T>,
    onSubmit: (value: TextFieldValue) -> T?,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    readOnly: Boolean,
    isError: Boolean,
    textStyle: TextStyle,
    colors: ChipTextFieldColors,
    keyboardOptions: KeyboardOptions,
    focusRequester: FocusRequester,
    interactionSource: MutableInteractionSource,
    onFocusChange: (isFocused: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (value.text.isEmpty() && (!enabled || readOnly)) {
        return
    }
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled, isError, interactionSource).value
    }

    fun tryAddNewChip(value: TextFieldValue): Boolean {
        return onSubmit(value)?.also { state.addChip(it) } != null
    }

    BasicTextField(
        value = value,
        onValueChange = filterNewLines { newValue, hasNewLine ->
            if (hasNewLine && newValue.text.isNotEmpty() && tryAddNewChip(newValue)) {
                onValueChange(TextFieldValue())
            } else {
                onValueChange(newValue)
            }
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                onFocusChange(it.isFocused)
                if (it.isFocused) {
                    state.focusTextField()
                }
            }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace) {
                    if (value.text.isEmpty() && state.chips.isNotEmpty()) {
                        // Remove previous chip
                        state.removeLastChip()
                        state.focusTextField()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle.copy(color = textColor),
        keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                if (value.text.isNotEmpty() && tryAddNewChip(value)) {
                    onValueChange(TextFieldValue())
                }
            }
        ),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(colors.cursorColor(isError).value),
    )
}

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
private fun <T : Chip> ChipItem(
    state: ChipTextFieldState<T>,
    focusRequester: FocusRequester,
    chip: T,
    enabled: Boolean,
    readOnly: Boolean,
    onRemoveRequest: () -> Unit,
    onFocusNextRequest: () -> Unit,
    onFocusChange: (isFocused: Boolean) -> Unit,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    textStyle: TextStyle,
    chipStyle: ChipStyle,
    chipLeadingIcon: @Composable (chip: T) -> Unit,
    chipTrailingIcon: @Composable (chip: T) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val shape by chipStyle.shape(
        enabled = enabled,
        interactionSource = interactionSource,
    )

    val borderWidth by chipStyle.borderWidth(
        enabled = enabled,
        interactionSource = interactionSource,
    )

    val borderColor by chipStyle.borderColor(
        enabled = enabled,
        interactionSource = interactionSource,
    )

    val textColor by chipStyle.textColor(
        enabled = enabled,
        interactionSource = interactionSource,
    )
    val chipTextStyle = remember(textColor) { textStyle.copy(color = textColor) }

    val backgroundColor by chipStyle.backgroundColor(
        enabled = enabled,
        interactionSource = interactionSource,
    )

    val cursorColor by chipStyle.cursorColor()

    val keyboardController = LocalSoftwareKeyboardController.current

    val editable = enabled && !readOnly

    DisposableEffect(chip) {
        onDispose {
            val chips = state.chips
            if (!chips.contains(chip)) {
                // The current chip is removed, onFocusChanged() will not get called,
                // free the focus manually
                onFocusChange(false)
            }
        }
    }

    ChipItemLayout(
        leadingIcon = {
            chipLeadingIcon(chip)
        },
        trailingIcon = {
            chipTrailingIcon(chip)
        },
        modifier = modifier
            .clip(shape = shape)
            .background(color = backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shape
            )
            .padding(borderWidth)
            .combinedClickable(
                enabled = enabled,
                onClick = {
                    if (editable) {
                        keyboardController?.show()
                        runCatching { focusRequester.requestFocus() }
                    }
                    onClick?.invoke()
                },
                onLongClick = {
                    onLongClick?.invoke()
                }
            ),
    ) {
        var canRemoveChip by remember { mutableStateOf(false) }
        BasicTextField(
            value = chip.textFieldValue,
            onValueChange = filterNewLines { value, hasNewLine ->
                chip.textFieldValue = value
                if (hasNewLine) {
                    onFocusNextRequest()
                }
            },
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChange(it.isFocused) }
                .onPreviewKeyEvent {
                    if (it.key == Key.Backspace) {
                        if (it.type == KeyEventType.KeyDown) {
                            canRemoveChip = chip.text.isEmpty()
                        } else if (it.type == KeyEventType.KeyUp) {
                            if (canRemoveChip) {
                                onRemoveRequest()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onFocusNextRequest() }),
            singleLine = false,
            enabled = !readOnly && enabled,
            readOnly = readOnly || !enabled,
            textStyle = chipTextStyle,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(cursorColor),
        )
    }
}

@Composable
private fun ChipItemLayout(
    leadingIcon: @Composable () -> Unit,
    trailingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            Box { leadingIcon() }
            Box { content() }
            Box { trailingIcon() }
        },
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth

        val leadingMeasurable = measurables[0]
        val contentMeasurable = measurables[1]
        val trailingMeasurable = measurables[2]

        var restWidth = maxWidth

        val leadingPlaceable = leadingMeasurable.measure(constraints = constraints)
        restWidth -= leadingPlaceable.width

        val trailingPlaceable = trailingMeasurable.measure(Constraints(maxWidth = restWidth))
        restWidth -= trailingPlaceable.width

        val contentPlaceable = contentMeasurable.measure(Constraints(maxWidth = restWidth))

        val width = leadingPlaceable.width + contentPlaceable.width + trailingPlaceable.width
        val height = maxOf(
            leadingPlaceable.height,
            contentPlaceable.height,
            trailingPlaceable.height,
        )

        val placeables = arrayOf(leadingPlaceable, contentPlaceable, trailingPlaceable)

        layout(width = width, height = height) {
            var x = 0
            for (placeable in placeables) {
                placeable.placeRelative(
                    x = x,
                    y = (height - placeable.height) / 2,
                )
                x += placeable.width
            }
        }
    }
}
