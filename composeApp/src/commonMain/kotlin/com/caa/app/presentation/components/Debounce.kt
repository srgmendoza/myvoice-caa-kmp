package com.caa.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class DebouncedAction(private val window: Duration = 350.milliseconds) {
    private var last: TimeSource.Monotonic.ValueTimeMark? = null
    fun fire(action: () -> Unit) {
        val now = TimeSource.Monotonic.markNow()
        val prev = last
        if (prev == null || now - prev >= window) {
            last = now
            action()
        }
    }
}

@Composable
fun rememberDebounce(windowMs: Long = 350L): DebouncedAction =
    remember(windowMs) { DebouncedAction(windowMs.milliseconds) }
