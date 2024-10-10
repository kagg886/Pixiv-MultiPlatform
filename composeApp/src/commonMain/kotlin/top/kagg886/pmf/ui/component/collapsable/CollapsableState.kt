/*
 * Copyright 2021 The Android Open Source Project
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
package top.kagg886.pmf.ui.component.collapsable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import kotlin.math.absoluteValue

/**
 * A state object that can be hoisted to control and observe the collapsable state. The state is
 * read and updated by [CollapsableTopBehavior].
 *
 * In most cases, this state will be created via [rememberCollapsableState].
 *
 * To update your view from this state you should do two things:
 * 1. Set [heightOffsetLimit] either by passing in the initial value ahead of time or calculating
 * it on layout.
 * 2. Read [heightOffset] and [collapsedFraction] to update your ui based on how far it has
 * collapsed.
 *
 * You can also optionally apply [Modifier.draggable] to allow dragging on the view itself to
 * expand and collapse it.
 *
 * @param initialHeightOffsetLimit the initial value for [heightOffsetLimit]
 * @param initialHeightOffset the initial value for [heightOffset]
 */
@Stable
class CollapsableState(
    initialHeightOffsetLimit: Float,
    initialHeightOffset: Float,
) {

    /**
     * The height offset limit in pixels, which represents the limit it is allowed to collapse to.
     * Note: This value is typically negative, where 0 means no collapsing will take place and a
     * negative value will be the distance it will collapse.
     *
     * Use this limit to coerce the [heightOffset] value when it's updated.
     */
    private var _heightOffsetLimit = mutableFloatStateOf(initialHeightOffsetLimit)

    var heightOffsetLimit: Float
        get() = _heightOffsetLimit.floatValue
        set(newLimit) {
            _heightOffsetLimit.floatValue = newLimit
            _heightOffset.floatValue = _heightOffset.floatValue.coerceIn(
                minimumValue = _heightOffsetLimit.floatValue,
                maximumValue = 0f,
            )
            animatable.updateBounds(
                lowerBound = newLimit,
                upperBound = 0f
            )
        }

    private var _heightOffset = mutableFloatStateOf(initialHeightOffset)

    /**
     * The current height offset in pixels. This height offset is applied to the fixed
     * height to control the displayed height when content is being scrolled.
     *
     * Updates to the [heightOffset] value are coerced between zero and [heightOffsetLimit].
     */
    var heightOffset: Float
        get() = _heightOffset.floatValue
        set(newOffset) {
            _heightOffset.floatValue = newOffset.coerceIn(
                minimumValue = _heightOffsetLimit.floatValue,
                maximumValue = 0f
            )
        }

    /**
     * A value that represents the collapsed height percentage.
     *
     * A `0.0` represents fully expanded, and `1.0` represents fully collapsed (computed * as
     * [heightOffset] / [heightOffsetLimit]).
     */
    val collapsedFraction: Float
        get() = if (_heightOffsetLimit.floatValue != 0f) {
            _heightOffset.floatValue / _heightOffsetLimit.floatValue
        } else {
            0f
        }


    /**
     * Drags [heightOffset] the given delta.
     *
     * TODO: expose?
     *
     * @param delta the delta to drag by
     * @return the delta that was consumed
     */
    internal fun drag(delta: Float): Float {
        val initialHeightOffset = _heightOffset.floatValue
        heightOffset += delta
        return _heightOffset.floatValue - initialHeightOffset
    }

    /**
     * Handles animating the [heightOffset].
     */
    private val animatable =
        Animatable(initialValue = initialHeightOffset, visibilityThreshold = 0.5f)

    /**
     * Flings the [heightOffset] the given velocity.
     *
     * TODO: expose?
     *
     * @param velocity the velocity to fling by
     * @param flingAnimationSpec the animation for the fling, passing null disables flinging
     * @param snapAnimationSpec the animation for snapping to the expanded or collapsed state after
     * flinging, passing null disables snapping
     *
     * @return the amount of the velocity that was consumed
     */
    internal suspend fun fling(
        velocity: Float,
        flingAnimationSpec: DecayAnimationSpec<Float>? = null,
        snapAnimationSpec: AnimationSpec<Float>? = null,
    ): Float {
        // Check if completely collapsed/expanded. If so, no need to settle and just return
        // Zero Velocity.
        if (_heightOffset.floatValue == 0f || _heightOffset.floatValue == _heightOffsetLimit.floatValue) {
            return 0f
        }
        if (flingAnimationSpec != null || snapAnimationSpec != null) {
            // snap animation to current value as it might have changed through other means since
            // last animation.
            animatable.snapTo(_heightOffset.floatValue)
        }

        var remainingVelocity = velocity
        // In case there is an initial velocity that was left after a previous user fling, animate to
        // continue the motion to expand or collapse.
        if (flingAnimationSpec != null) {
            if (velocity.absoluteValue > 1f) {
                remainingVelocity = animatable.animateDecay(
                    initialVelocity = velocity,
                    animationSpec = flingAnimationSpec,
                ) {
                    _heightOffset.floatValue = value
                }.endState.velocity
            }
        }
        // Snap if animation specs were provided.
        if (snapAnimationSpec != null) {
            animatable.animateTo(
                targetValue = if (collapsedFraction < 0.5f) 0f else _heightOffsetLimit.floatValue,
                animationSpec = snapAnimationSpec
            ) {
                _heightOffset.floatValue = value
            }
        }
        return velocity - remainingVelocity
    }

    /**
     * Sets [heightOffset] to the fully expanded value
     */
    fun expand() {
        _heightOffset.floatValue = 0f
    }

    /**
     * Sets [heightOffset] to the fully collapsed value
     */
    fun collapse() {
        _heightOffset.floatValue = _heightOffsetLimit.floatValue
    }

    /**
     * Animates expanding [heightOffset] from the current value
     *
     * @param animationSpec The animation spec to use, defaults to [spring]
     */
    suspend fun animateExpand(animationSpec: AnimationSpec<Float> = spring()) {
        snap(target = 0f, animationSpec = animationSpec)
    }

    /**
     * Animates collapsing [heightOffset] from the current value
     *
     * @param animationSpec The animation spec to use, defaults to [spring]
     */
    suspend fun animateCollapse(animationSpec: AnimationSpec<Float> = spring()) {
        snap(target = _heightOffsetLimit.floatValue, animationSpec = animationSpec)
    }

    private suspend fun snap(
        target: Float,
        animationSpec: AnimationSpec<Float>
    ) {
        if (_heightOffset.floatValue == target) return
        animatable.snapTo(_heightOffset.floatValue)
        animatable.animateTo(targetValue = target, animationSpec = animationSpec) {
            _heightOffset.floatValue = value
        }
    }

    companion object {
        /**
         * The default [Saver] implementation for [CollapsableState].
         */
        val Saver: Saver<CollapsableState, *> = listSaver(
            save = { listOf(it.heightOffsetLimit, it.heightOffset) },
            restore = {
                CollapsableState(
                    initialHeightOffsetLimit = it[0],
                    initialHeightOffset = it[1],
                )
            }
        )
    }
}

/**
 * Remembers a [CollapsableState].
 */
@Composable
fun rememberCollapsableState(): CollapsableState {
    return rememberSaveable(saver = CollapsableState.Saver) {
        CollapsableState(initialHeightOffset = 0f, initialHeightOffsetLimit = 0f)
    }
}

/**
 * Remembers a [CollapsableState].
 *
 * @param initialHeightOffsetLimit the initial [CollapsableState.heightOffsetLimit] in pixels. This
 * is useful for cases where it is know ahead of time instead of calculated on layout.
 */
@Composable
fun rememberCollapsableState(initialHeightOffsetLimit: Float = 0f): CollapsableState {
    return rememberSaveable(saver = CollapsableState.Saver) {
        CollapsableState(
            initialHeightOffset = 0f,
            initialHeightOffsetLimit = initialHeightOffsetLimit
        )
    }
}
