package com.buka.mobile.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val VIEW_MODE_PREFS = "buka_view_modes"

@Composable
internal fun rememberDisplayModePreference(
    key: String,
    default: VisualDisplayMode = VisualDisplayMode.GRID,
): MutableState<VisualDisplayMode> {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(VIEW_MODE_PREFS, Context.MODE_PRIVATE)
    }
    val state = remember(prefs, key) {
        val saved = prefs.getString(key, null)
        mutableStateOf(
            when (saved) {
                VisualDisplayMode.LIST.name -> VisualDisplayMode.LIST
                VisualDisplayMode.GRID.name -> VisualDisplayMode.GRID
                else -> default
            },
        )
    }
    LaunchedEffect(key, state.value) {
        prefs.edit().putString(key, state.value.name).apply()
    }
    return state
}
