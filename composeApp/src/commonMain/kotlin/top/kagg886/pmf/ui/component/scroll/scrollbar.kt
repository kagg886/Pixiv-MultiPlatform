/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.kagg886.pmf.ui.component.scroll

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.kagg886.pmf.ui.component.scroll.v2.*

/**
 * [CompositionLocal] used to pass [ScrollbarStyle] down the tree.
 * This value is typically set in some "Theme" composable function
 * (DesktopTheme, MaterialTheme)
 */
val LocalScrollbarStyle = staticCompositionLocalOf { defaultScrollbarStyle() }

/**
 * Defines visual style of scrollbars (thickness, shapes, colors, etc).
 * Can be passed as a parameter of scrollbar through [LocalScrollbarStyle]
 */
@Immutable
data class ScrollbarStyle(
    val minimalHeight: Dp,
    val thickness: Dp,
    val shape: Shape,
    val hoverDurationMillis: Int,
    val unhoverColor: Color,
    val hoverColor: Color,
)

/**
 * Simple default [ScrollbarStyle] without applying MaterialTheme.
 */
fun defaultScrollbarStyle() = ScrollbarStyle(
    minimalHeight = 16.dp,
    thickness = 8.dp,
    shape = RoundedCornerShape(4.dp),
    hoverDurationMillis = 300,
    unhoverColor = Color.Black.copy(alpha = 0.12f),
    hoverColor = Color.Black.copy(alpha = 0.50f),
)

/**
 * Vertical scrollbar that can be attached to some scrollable
 * component (ScrollableColumn, LazyColumn) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 *
 * @param adapter [ScrollbarAdapter] that will be used to communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the bottom of the container.
 * It is usually used in pair with `LazyColumn(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Deprecated(
    "Use VerticalScrollbar(" +
        "adapter: top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter)" +
        " instead",
)
@Composable
fun VerticalScrollbar(
    @Suppress("DEPRECATION") adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = OldScrollbar(
    adapter,
    modifier,
    reverseLayout,
    style,
    interactionSource,
    isVertical = true,
)

/**
 * Horizontal scrollbar that can be attached to some scrollable
 * component (Modifier.verticalScroll(), LazyRow) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.horizontalScroll(state)) {
 *             ...
 *         }
 *
 *         HorizontalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
 *         )
 *     }
 *
 * @param adapter [ScrollbarAdapter] that will be used to communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the end of the container.
 * It is usually used in pair with `LazyRow(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Deprecated(
    "Use HorizontalScrollbar(" +
        "adapter: top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter) instead",
)
@Composable
fun HorizontalScrollbar(
    @Suppress("DEPRECATION") adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = OldScrollbar(
    adapter,
    modifier,
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) !reverseLayout else reverseLayout,
    style,
    interactionSource,
    isVertical = false,
)

@Suppress("DEPRECATION")
@Composable
private fun OldScrollbar(
    oldAdapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean,
    style: ScrollbarStyle,
    interactionSource: MutableInteractionSource,
    isVertical: Boolean,
) = OldOrNewScrollbar(
    oldOrNewAdapter = oldAdapter,
    newScrollbarAdapterFactory = ScrollbarAdapter::asNewAdapter,
    modifier = modifier,
    reverseLayout = reverseLayout,
    style = style,
    interactionSource = interactionSource,
    isVertical = isVertical,
)

/**
 * Vertical scrollbar that can be attached to some scrollable
 * component (ScrollableColumn, LazyColumn) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 *
 * @param adapter [androidx.compose.foundation.v2.ScrollbarAdapter] that will be used to
 * communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the bottom of the container.
 * It is usually used in pair with `LazyColumn(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Composable
fun VerticalScrollbar(
    adapter: top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = NewScrollbar(
    newAdapter = adapter,
    modifier,
    reverseLayout,
    style,
    interactionSource,
    isVertical = true,
)

/**
 * Horizontal scrollbar that can be attached to some scrollable
 * component (Modifier.verticalScroll(), LazyRow) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         HorizontalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
 *         )
 *     }
 *
 * @param adapter [androidx.compose.foundation.v2.ScrollbarAdapter] that will be used to
 * communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the end of the container.
 * It is usually used in pair with `LazyRow(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Composable
fun HorizontalScrollbar(
    adapter: top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = NewScrollbar(
    newAdapter = adapter,
    modifier,
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) !reverseLayout else reverseLayout,
    style,
    interactionSource,
    isVertical = false,
)

@Composable
private fun NewScrollbar(
    newAdapter: top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean,
    style: ScrollbarStyle,
    interactionSource: MutableInteractionSource,
    isVertical: Boolean,
) = OldOrNewScrollbar(
    oldOrNewAdapter = newAdapter,
    newScrollbarAdapterFactory = { adapter, _ -> adapter },
    modifier = modifier,
    reverseLayout = reverseLayout,
    style = style,
    interactionSource = interactionSource,
    isVertical = isVertical,
)

private typealias NewScrollbarAdapterFactory<T> = (
    adapter: T,
    trackSize: Int,
) -> top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter

/**
 * The actual implementation of the scrollbar.
 *
 * Takes the scroll adapter (old or new) and a function that converts it to the new scrollbar
 * adapter interface. This allows both the old (left for backwards compatibility) and new
 * implementations to use the same code.
 */
@Composable
internal fun <T> OldOrNewScrollbar(
    oldOrNewAdapter: T,
    // We need an adapter factory because we can't convert an old to a new
    // adapter until we have the track/container size
    newScrollbarAdapterFactory: NewScrollbarAdapterFactory<T>,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean,
    style: ScrollbarStyle,
    interactionSource: MutableInteractionSource,
    isVertical: Boolean,
) = with(LocalDensity.current) {
    val dragInteraction = remember { mutableStateOf<DragInteraction.Start?>(null) }
    DisposableEffect(interactionSource) {
        onDispose {
            dragInteraction.value?.let { interaction ->
                interactionSource.tryEmit(DragInteraction.Cancel(interaction))
                dragInteraction.value = null
            }
        }
    }

    var containerSize by remember { mutableStateOf(0) }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isHighlighted by remember {
        derivedStateOf {
            isHovered || dragInteraction.value is DragInteraction.Start
        }
    }

    val minimalHeight = style.minimalHeight.toPx()

    val adapter = remember(oldOrNewAdapter, containerSize) {
        newScrollbarAdapterFactory(oldOrNewAdapter, containerSize)
    }
    val coroutineScope = rememberCoroutineScope()
    val sliderAdapter = remember(
        adapter,
        containerSize,
        minimalHeight,
        reverseLayout,
        isVertical,
        coroutineScope,
    ) {
        SliderAdapter(adapter, containerSize, minimalHeight, reverseLayout, isVertical, coroutineScope)
    }

    val scrollThickness = style.thickness.roundToPx()
    val measurePolicy = if (isVertical) {
        remember(sliderAdapter, scrollThickness) {
            verticalMeasurePolicy(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    } else {
        remember(sliderAdapter, scrollThickness) {
            horizontalMeasurePolicy(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    }

    val color by animateColorAsState(
        if (isHighlighted) style.hoverColor else style.unhoverColor,
        animationSpec = TweenSpec(durationMillis = style.hoverDurationMillis),
    )

    val isVisible = sliderAdapter.thumbSize < containerSize

    Layout(
        {
            Box(
                Modifier
                    .background(if (isVisible) color else Color.Transparent, style.shape)
                    .scrollbarDrag(
                        interactionSource = interactionSource,
                        draggedInteraction = dragInteraction,
                        sliderAdapter = sliderAdapter,
                    ),
            )
        },
        modifier
            .hoverable(interactionSource = interactionSource)
            .scrollOnPressTrack(isVertical, reverseLayout, sliderAdapter),
        measurePolicy,
    )
}

/**
 * Adapts an old [ScrollbarAdapter] to the new interface, under the assumption that the
 * track size is equal to the viewport size.
 */
private class OldScrollbarAdapterAsNew(
    @Suppress("DEPRECATION") val oldAdapter: ScrollbarAdapter,
    private val trackSize: Int,
) : top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter {

    override val scrollOffset: Double
        get() = oldAdapter.scrollOffset.toDouble()

    override val contentSize: Double
        get() = (oldAdapter.maxScrollOffset(trackSize) + trackSize).toDouble()

    override val viewportSize: Double
        get() = trackSize.toDouble()

    override suspend fun scrollTo(scrollOffset: Double) {
        oldAdapter.scrollTo(trackSize, scrollOffset.toFloat())
    }
}

/**
 * Converts an instance of the old scrollbar adapter to a new one.
 *
 * If the old one is in fact just a [NewScrollbarAdapterAsOld], then simply unwrap it.
 * This allows users that simply passed our own (old) implementations back to
 * us to seamlessly use the new implementations, and enjoy all their benefits.
 */
@Suppress("DEPRECATION")
private fun ScrollbarAdapter.asNewAdapter(
    trackSize: Int,
): top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter = if (this is NewScrollbarAdapterAsOld) {
    this.newAdapter // Just unwrap
} else {
    OldScrollbarAdapterAsNew(this, trackSize)
}

/**
 * Adapts a new scrollbar adapter to the old interface.
 */
@Suppress("DEPRECATION")
private class NewScrollbarAdapterAsOld(
    val newAdapter: top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter,
) : ScrollbarAdapter {

    override val scrollOffset: Float
        get() = newAdapter.scrollOffset.toFloat()

    override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
        newAdapter.scrollTo(scrollOffset.toDouble())
    }

    override fun maxScrollOffset(containerSize: Int): Float = newAdapter.maxScrollOffset.toFloat()
}

/**
 * Converts an instance of the new scrollbar adapter to an old one.
 */
@Suppress("DEPRECATION")
private fun top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter.asOldAdapter(): ScrollbarAdapter = if (this is OldScrollbarAdapterAsNew) {
    this.oldAdapter // Just unwrap
} else {
    NewScrollbarAdapterAsOld(this)
}

/**
 * Create and [remember] (old) [ScrollbarAdapter] for scrollable container and current instance of
 * [scrollState]
 */
@Deprecated(
    message = "Use rememberScrollbarAdapter instead",
    replaceWith = ReplaceWith(
        expression = "rememberScrollbarAdapter(scrollState)",
        "androidx.compose.foundation.rememberScrollbarAdapter",
    ),
)
@Suppress("DEPRECATION")
@Composable
fun rememberOldScrollbarAdapter(
    scrollState: ScrollState,
): ScrollbarAdapter = remember(scrollState) {
    OldScrollbarAdapter(scrollState)
}

/**
 * Create and [remember] (old) [ScrollbarAdapter] for lazy scrollable container and current instance
 * of [scrollState]
 */
@Deprecated(
    message = "Use rememberScrollbarAdapter instead",
    replaceWith = ReplaceWith(
        expression = "rememberScrollbarAdapter(scrollState)",
        "androidx.compose.foundation.rememberScrollbarAdapter",
    ),
)
@Suppress("DEPRECATION")
@Composable
fun rememberOldScrollbarAdapter(
    scrollState: LazyListState,
): ScrollbarAdapter = remember(scrollState) {
    OldScrollbarAdapter(scrollState)
}

/**
 * ScrollbarAdapter for Modifier.verticalScroll and Modifier.horizontalScroll
 *
 * [scrollState] is instance of [ScrollState] which is used by scrollable component
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@Deprecated(
    message = "Use ScrollbarAdapter() instead",
    replaceWith = ReplaceWith(
        expression = "ScrollbarAdapter(scrollState)",
        "androidx.compose.foundation.ScrollbarAdapter",
    ),
)
@Suppress("DEPRECATION")
fun OldScrollbarAdapter(
    scrollState: ScrollState,
): ScrollbarAdapter = ScrollbarAdapter(scrollState).asOldAdapter()

/**
 * ScrollbarAdapter for lazy lists.
 *
 * [scrollState] is instance of [LazyListState] which is used by scrollable component
 *
 * Example:
 *     Box(Modifier.fillMaxSize()) {
 *         val state = rememberLazyListState()
 *
 *         LazyColumn(state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@Deprecated(
    message = "Use ScrollbarAdapter() instead",
    replaceWith = ReplaceWith(
        expression = "ScrollbarAdapter(scrollState)",
        "androidx.compose.foundation.ScrollbarAdapter",
    ),
)
@Suppress("DEPRECATION")
fun OldScrollbarAdapter(
    scrollState: LazyListState,
): ScrollbarAdapter = ScrollbarAdapter(scrollState).asOldAdapter()

/**
 * Create and [remember] [androidx.compose.foundation.v2.ScrollbarAdapter] for
 * scrollable container with the given instance [ScrollState].
 */
@Composable
fun rememberScrollbarAdapter(
    scrollState: ScrollState,
): top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * Create and [remember] [androidx.compose.foundation.v2.ScrollbarAdapter] for
 * lazy scrollable container with the given instance [LazyListState].
 */
@Composable
fun rememberScrollbarAdapter(
    scrollState: LazyListState,
): top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

@Composable
fun rememberScrollbarAdapter(
    scrollState: LazyStaggeredGridState,
): top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * Create and [remember] [androidx.compose.foundation.v2.ScrollbarAdapter] for lazy grid with
 * the given instance of [LazyGridState].
 */
@Composable
fun rememberScrollbarAdapter(
    scrollState: LazyGridState,
): top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * ScrollbarAdapter for Modifier.verticalScroll and Modifier.horizontalScroll
 *
 * [scrollState] is instance of [ScrollState] which is used by scrollable component
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
fun ScrollbarAdapter(
    scrollState: ScrollState,
): top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter = ScrollableScrollbarAdapter(scrollState)

/**
 * ScrollbarAdapter for lazy lists.
 *
 * [scrollState] is instance of [LazyListState] which is used by scrollable component
 *
 * Example:
 *     Box(Modifier.fillMaxSize()) {
 *         val state = rememberLazyListState()
 *
 *         LazyColumn(state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
fun ScrollbarAdapter(
    scrollState: LazyListState,
): top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter = LazyListScrollbarAdapter(scrollState)

fun ScrollbarAdapter(
    scrollState: LazyStaggeredGridState,
): top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter = LazyStaggerGirdListScrollbarAdapter(scrollState)

/**
 * ScrollbarAdapter for lazy grids.
 *
 * [scrollState] is instance of [LazyGridState] which is used by scrollable component
 *
 * Example:
 *     Box(Modifier.fillMaxSize()) {
 *         val state = rememberLazyGridState()
 *
 *         LazyVerticalGrid(columns = ..., state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
fun ScrollbarAdapter(
    scrollState: LazyGridState,
): top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter = LazyGridScrollbarAdapter(scrollState)

/**
 * Defines how to scroll the scrollable component
 */
@Deprecated("Use top.kagg886.pmf.ui.component.scroll.v2.ScrollbarAdapter instead")
interface ScrollbarAdapter {

    /**
     * Scroll offset of the content inside the scrollable component.
     * Offset "100" means that the content is scrolled by 100 pixels from the start.
     */
    val scrollOffset: Float

    /**
     * Instantly jump to [scrollOffset] in pixels
     *
     * @param containerSize size of the scrollable container
     *  (for example, it is height of ScrollableColumn if we use VerticalScrollbar)
     * @param scrollOffset target value in pixels to jump to,
     *  value will be coerced to 0..maxScrollOffset
     */
    suspend fun scrollTo(containerSize: Int, scrollOffset: Float)

    /**
     * Maximum scroll offset of the content inside the scrollable component
     *
     * @param containerSize size of the scrollable component
     *  (for example, it is height of ScrollableColumn if we use VerticalScrollbar)
     */
    fun maxScrollOffset(containerSize: Int): Float
}

private val SliderAdapter.thumbPixelRange: IntRange
    get() {
        val start = position.roundToInt()
        val endExclusive = start + thumbSize.roundToInt()

        return (start until endExclusive)
    }

private val IntRange.size get() = last + 1 - first

private fun verticalMeasurePolicy(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int,
) = MeasurePolicy { measurables, constraints ->
    setContainerSize(constraints.maxHeight)
    val pixelRange = sliderAdapter.thumbPixelRange
    val placeable = measurables.first().measure(
        Constraints.fixed(
            constraints.constrainWidth(scrollThickness),
            pixelRange.size,
        ),
    )
    layout(placeable.width, constraints.maxHeight) {
        placeable.place(0, pixelRange.first)
    }
}

private fun horizontalMeasurePolicy(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int,
) = MeasurePolicy { measurables, constraints ->
    setContainerSize(constraints.maxWidth)
    val pixelRange = sliderAdapter.thumbPixelRange
    val placeable = measurables.first().measure(
        Constraints.fixed(
            pixelRange.size,
            constraints.constrainHeight(scrollThickness),
        ),
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(pixelRange.first, 0)
    }
}

private fun Modifier.scrollbarDrag(
    interactionSource: MutableInteractionSource,
    draggedInteraction: MutableState<DragInteraction.Start?>,
    sliderAdapter: SliderAdapter,
): Modifier = composed {
    val currentInteractionSource by rememberUpdatedState(interactionSource)
    val currentDraggedInteraction by rememberUpdatedState(draggedInteraction)
    val currentSliderAdapter by rememberUpdatedState(sliderAdapter)

    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val interaction = DragInteraction.Start()
            currentInteractionSource.tryEmit(interaction)
            currentDraggedInteraction.value = interaction
            currentSliderAdapter.onDragStarted()
            val isSuccess = drag(down.id) { change ->
                currentSliderAdapter.onDragDelta(change.positionChange())
                change.consume()
            }
            val finishInteraction = if (isSuccess) {
                DragInteraction.Stop(interaction)
            } else {
                DragInteraction.Cancel(interaction)
            }
            currentInteractionSource.tryEmit(finishInteraction)
            currentDraggedInteraction.value = null
        }
    }
}

private fun Modifier.scrollOnPressTrack(
    isVertical: Boolean,
    reverseLayout: Boolean,
    sliderAdapter: SliderAdapter,
) = composed {
    val coroutineScope = rememberCoroutineScope()
    val scroller = remember(sliderAdapter, coroutineScope, reverseLayout) {
        TrackPressScroller(coroutineScope, sliderAdapter, reverseLayout)
    }
    Modifier.pointerInput(scroller) {
        detectScrollViaTrackGestures(
            isVertical = isVertical,
            scroller = scroller,
        )
    }
}

/**
 * Responsible for scrolling when the scrollbar track is pressed (outside the thumb).
 */
private class TrackPressScroller(
    private val coroutineScope: CoroutineScope,
    private val sliderAdapter: SliderAdapter,
    private val reverseLayout: Boolean,
) {

    /**
     * The current direction of scroll (1: down/right, -1: up/left, 0: not scrolling)
     */
    private var direction = 0

    /**
     * The currently pressed location (in pixels) on the scrollable axis.
     */
    private var offset: Float? = null

    /**
     * The job that keeps scrolling while the track is pressed.
     */
    private var job: Job? = null

    /**
     * Calculates the direction of scrolling towards the given offset (in pixels).
     */
    private fun directionOfScrollTowards(offset: Float): Int {
        val pixelRange = sliderAdapter.thumbPixelRange
        return when {
            offset < pixelRange.first -> if (reverseLayout) 1 else -1
            offset > pixelRange.last -> if (reverseLayout) -1 else 1
            else -> 0
        }
    }

    /**
     * Scrolls once towards the current offset, if it matches the direction of the current gesture.
     */
    private suspend fun scrollTowardsCurrentOffset() {
        offset?.let {
            val currentDirection = directionOfScrollTowards(it)
            if (currentDirection != direction) {
                return
            }
            with(sliderAdapter.adapter) {
                scrollTo(scrollOffset + currentDirection * viewportSize)
            }
        }
    }

    /**
     * Starts the job that scrolls continuously towards the current offset.
     */
    private fun startScrolling() {
        job?.cancel()
        job = coroutineScope.launch {
            scrollTowardsCurrentOffset()
            delay(DelayBeforeSecondScrollOnTrackPress)
            while (true) {
                scrollTowardsCurrentOffset()
                delay(DelayBetweenScrollsOnTrackPress)
            }
        }
    }

    /**
     * Invoked on the first press for a gesture.
     */
    fun onPress(offset: Float) {
        this.offset = offset
        this.direction = directionOfScrollTowards(offset)

        if (direction != 0) {
            startScrolling()
        }
    }

    /**
     * Invoked when the pointer moves while pressed during the gesture.
     */
    fun onMovePressed(offset: Float) {
        this.offset = offset
    }

    /**
     * Cleans up when the gesture finishes.
     */
    private fun cleanupAfterGesture() {
        job?.cancel()
        direction = 0
        offset = null
    }

    /**
     * Invoked when the button is released.
     */
    fun onRelease() {
        cleanupAfterGesture()
    }

    /**
     * Invoked when the gesture is cancelled.
     */
    fun onGestureCancelled() {
        cleanupAfterGesture()
        // Maybe revert to the initial position?
    }
}

/**
 * Detects the pointer events relevant for the "scroll by pressing on the track outside the thumb"
 * gesture and calls the corresponding methods in the [scroller].
 */
private suspend fun PointerInputScope.detectScrollViaTrackGestures(
    isVertical: Boolean,
    scroller: TrackPressScroller,
) {
    fun Offset.onScrollAxis() = if (isVertical) y else x

    awaitEachGesture {
        val down = awaitFirstDown()
        scroller.onPress(down.position.onScrollAxis())

        while (true) {
            val drag =
                if (isVertical) {
                    awaitVerticalDragOrCancellation(down.id)
                } else {
                    awaitHorizontalDragOrCancellation(down.id)
                }

            if (drag == null) {
                scroller.onGestureCancelled()
                break
            } else if (!drag.pressed) {
                scroller.onRelease()
                break
            } else {
                scroller.onMovePressed(drag.position.onScrollAxis())
            }
        }
    }
}

/**
 * The delay between the 1st and 2nd scroll while the scrollbar track is pressed outside the thumb.
 */
internal const val DelayBeforeSecondScrollOnTrackPress: Long = 300L

/**
 * The delay between each subsequent (after the 2nd) scroll while the scrollbar track is pressed
 * outside the thumb.
 */
internal const val DelayBetweenScrollsOnTrackPress: Long = 100L
