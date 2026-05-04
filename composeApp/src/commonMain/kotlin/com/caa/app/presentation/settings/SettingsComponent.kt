package com.caa.app.presentation.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.caa.app.domain.model.AppSettings
import com.caa.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface SettingsComponent {
    val state: Value<AppSettings>
    fun onDebounceChanged(value: Long)
    fun onBack()
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val repository: SettingsRepository,
    private val onClose: () -> Unit
) : SettingsComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main)
    private val _state = MutableValue(AppSettings())
    override val state: Value<AppSettings> = _state

    init {
        scope.launch {
            repository.observe().collect { s -> _state.update { s } }
        }
    }

    override fun onDebounceChanged(value: Long) {
        scope.launch { repository.setDebounceMs(value) }
    }

    override fun onBack() = onClose()
}
