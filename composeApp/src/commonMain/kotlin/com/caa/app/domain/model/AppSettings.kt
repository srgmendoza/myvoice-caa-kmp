package com.caa.app.domain.model

data class AppSettings(
    val debounceMs: Long = 350L
) {
    companion object {
        const val DEBOUNCE_MIN = 100L
        const val DEBOUNCE_MAX = 1500L
        const val DEBOUNCE_STEP = 50L
    }
}
