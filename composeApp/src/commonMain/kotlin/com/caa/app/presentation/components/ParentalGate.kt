package com.caa.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import caa_kmp.composeapp.generated.resources.Res
import caa_kmp.composeapp.generated.resources.action_cancel
import caa_kmp.composeapp.generated.resources.action_enter
import caa_kmp.composeapp.generated.resources.gate_answer
import caa_kmp.composeapp.generated.resources.gate_challenge
import caa_kmp.composeapp.generated.resources.gate_title
import caa_kmp.composeapp.generated.resources.gate_wait
import caa_kmp.composeapp.generated.resources.gate_wrong
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random
import kotlin.time.TimeSource

private const val MAX_ATTEMPTS = 3
private const val COOLDOWN_MS = 5000L

/**
 * Process-lifetime lockout state, shared across dialog instances so that
 * dismissing and reopening the gate does NOT reset attempts or an active cooldown.
 * (Reset on process death is acceptable.)
 */
private object GateLockState {
    var attempts: Int = 0
    private var lockStart: TimeSource.Monotonic.ValueTimeMark? = null

    fun startLock() {
        lockStart = TimeSource.Monotonic.markNow()
    }

    fun remainingLockMs(): Long {
        val start = lockStart ?: return 0L
        val remaining = COOLDOWN_MS - start.elapsedNow().inWholeMilliseconds
        if (remaining <= 0L) {
            lockStart = null
            return 0L
        }
        return remaining
    }

    fun reset() {
        attempts = 0
        lockStart = null
    }
}

@Composable
fun ParentalGateDialog(
    onPass: () -> Unit,
    onDismiss: () -> Unit
) {
    var challenge by remember { mutableStateOf(generateChallenge()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var attemptsShown by remember { mutableStateOf(GateLockState.attempts) }
    var remainingMs by remember { mutableStateOf(GateLockState.remainingLockMs()) }
    // Incremented each time a new lock starts, to restart the countdown effect.
    var lockTick by remember { mutableStateOf(0) }

    LaunchedEffect(lockTick) {
        remainingMs = GateLockState.remainingLockMs()
        while (remainingMs > 0L) {
            delay(minOf(1000L, remainingMs))
            remainingMs = GateLockState.remainingLockMs()
        }
    }

    val locked = remainingMs > 0L
    val secondsLeft = ((remainingMs + 999) / 1000).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.gate_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.gate_challenge, challenge.a, challenge.b))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit); error = false },
                    label = { Text(stringResource(Res.string.gate_answer)) },
                    isError = error,
                    enabled = !locked,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (locked) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.gate_wait, secondsLeft),
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (error) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(Res.string.gate_wrong, attemptsShown, MAX_ATTEMPTS),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (locked || GateLockState.remainingLockMs() > 0L) return@TextButton
                    if (input.toIntOrNull() == challenge.result) {
                        GateLockState.reset()
                        onPass()
                    } else {
                        GateLockState.attempts++
                        attemptsShown = GateLockState.attempts
                        error = true
                        input = ""
                        if (GateLockState.attempts >= MAX_ATTEMPTS) {
                            GateLockState.attempts = 0
                            attemptsShown = 0
                            GateLockState.startLock()
                            challenge = generateChallenge()
                            lockTick++
                        }
                    }
                },
                enabled = !locked
            ) { Text(stringResource(Res.string.action_enter)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        }
    )
}

private data class Challenge(val a: Int, val b: Int) {
    val result: Int = a + b
}

/** Sum challenge per spec: two addends whose total is in 11..19. */
private fun generateChallenge(): Challenge {
    val a = Random.nextInt(4, 10) // 4..9
    val minB = (11 - a).coerceAtLeast(2)
    val maxB = 19 - a
    val b = Random.nextInt(minB, maxB + 1)
    return Challenge(a, b)
}
