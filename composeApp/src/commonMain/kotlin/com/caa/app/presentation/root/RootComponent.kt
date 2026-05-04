package com.caa.app.presentation.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.caa.app.domain.repository.PictogramRepository
import com.caa.app.domain.repository.SettingsRepository
import com.caa.app.platform.tts.SpeechEngine
import com.caa.app.presentation.edit.DefaultEditComponent
import com.caa.app.presentation.edit.EditComponent
import com.caa.app.presentation.grid.DefaultGridComponent
import com.caa.app.presentation.grid.GridComponent
import com.caa.app.presentation.settings.DefaultSettingsComponent
import com.caa.app.presentation.settings.SettingsComponent
import kotlinx.serialization.Serializable

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        data class Grid(val component: GridComponent) : Child()
        data class Edit(val component: EditComponent) : Child()
        data class Settings(val component: SettingsComponent) : Child()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val repository: PictogramRepository,
    private val settings: SettingsRepository,
    private val speech: SpeechEngine
) : RootComponent, ComponentContext by componentContext {

    @Serializable
    private sealed interface Config {
        @Serializable data object Grid : Config
        @Serializable data object Edit : Config
        @Serializable data object Settings : Config
    }

    private val nav = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = nav,
            serializer = Config.serializer(),
            initialConfiguration = Config.Grid,
            handleBackButton = true,
            childFactory = ::createChild
        )

    @OptIn(DelicateDecomposeApi::class)
    private fun createChild(config: Config, ctx: ComponentContext): RootComponent.Child =
        when (config) {
            Config.Grid -> RootComponent.Child.Grid(
                DefaultGridComponent(ctx, repository, settings, speech) { nav.push(Config.Edit) }
            )
            Config.Edit -> RootComponent.Child.Edit(
                DefaultEditComponent(ctx, repository, { nav.push(Config.Settings) }, { nav.pop() })
            )
            Config.Settings -> RootComponent.Child.Settings(
                DefaultSettingsComponent(ctx, settings) { nav.pop() }
            )
        }
}
