package top.kagg886.pmf.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.SettingsBuilder
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

fun <STATE : Any, SIDE_EFFECT : Any> ViewModel.container(
    initialState: STATE,
    buildSettings: SettingsBuilder.() -> Unit = {},
    onCreate: (suspend Syntax<STATE, SIDE_EFFECT>.() -> Unit)? = null,
): Container<STATE, SIDE_EFFECT> = viewModelScope.container(initialState, buildSettings, onCreate)
