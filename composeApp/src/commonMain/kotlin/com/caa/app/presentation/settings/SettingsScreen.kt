package com.caa.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.caa.app.domain.model.AppSettings
import com.caa.app.presentation.theme.CaaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state by component.state.subscribeAsState()
    var localValue by remember(state.debounceMs) { mutableStateOf(state.debounceMs.toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = component::onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(CaaSpacing.sp16)
        ) {
            Text(
                text = "Tiempo entre toques",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(CaaSpacing.sp4))
            Text(
                text = "Evita pulsaciones repetidas accidentales. Más tiempo = más protección, menos respuesta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(CaaSpacing.sp16))

            val steps = ((AppSettings.DEBOUNCE_MAX - AppSettings.DEBOUNCE_MIN) / AppSettings.DEBOUNCE_STEP).toInt() - 1

            Slider(
                value = localValue,
                onValueChange = { localValue = it },
                onValueChangeFinished = {
                    val snapped = ((localValue / AppSettings.DEBOUNCE_STEP).toLong()
                        * AppSettings.DEBOUNCE_STEP)
                        .coerceIn(AppSettings.DEBOUNCE_MIN, AppSettings.DEBOUNCE_MAX)
                    component.onDebounceChanged(snapped)
                },
                valueRange = AppSettings.DEBOUNCE_MIN.toFloat()..AppSettings.DEBOUNCE_MAX.toFloat(),
                steps = steps
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${AppSettings.DEBOUNCE_MIN} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${localValue.toLong()} ms",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${AppSettings.DEBOUNCE_MAX} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
