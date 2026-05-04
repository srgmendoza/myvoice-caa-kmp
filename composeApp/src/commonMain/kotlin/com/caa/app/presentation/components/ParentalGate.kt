package com.caa.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val MAX_ATTEMPTS = 3
private const val COOLDOWN_MS = 5000L

@Composable
fun ParentalGateDialog(
    onPass: () -> Unit,
    onDismiss: () -> Unit
) {
    var challenge by remember { mutableStateOf(generateChallenge()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    var lockedUntilMs by remember { mutableStateOf(0L) }
    var nowMs by remember { mutableStateOf(0L) }

    LaunchedEffect(lockedUntilMs) {
        while (lockedUntilMs > nowMs) {
            nowMs += 1000
            delay(1000)
        }
    }

    val locked = lockedUntilMs > nowMs
    val secondsLeft = ((lockedUntilMs - nowMs).coerceAtLeast(0) / 1000).toInt() + 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modo padres") },
        text = {
            Column {
                Text("Resuelve: ${challenge.a} × ${challenge.b}")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit); error = false },
                    label = { Text("Respuesta") },
                    isError = error,
                    enabled = !locked,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (locked) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Espera $secondsLeft s",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (error) {
                    Spacer(Modifier.height(4.dp))
                    Text("Incorrecto (intento $attempts/$MAX_ATTEMPTS)", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (locked) return@TextButton
                    if (input.toIntOrNull() == challenge.result) {
                        onPass()
                    } else {
                        attempts++
                        error = true
                        input = ""
                        if (attempts >= MAX_ATTEMPTS) {
                            lockedUntilMs = nowMs + COOLDOWN_MS
                            attempts = 0
                            challenge = generateChallenge()
                        }
                    }
                },
                enabled = !locked
            ) { Text("Entrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private data class Challenge(val a: Int, val b: Int) {
    val result: Int = a * b
}

private fun generateChallenge(): Challenge =
    Challenge(Random.nextInt(6, 13), Random.nextInt(6, 13))
