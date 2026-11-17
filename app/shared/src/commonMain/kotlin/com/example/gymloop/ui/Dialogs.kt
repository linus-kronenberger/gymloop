package com.example.gymloop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CreateExerciseDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Übung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Übungsname (z. B. Bankdrücken)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = !isSubmitting && name.isNotBlank()
            ) {
                Text("Anlegen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun AddRecordDialog(
    exerciseName: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Double, repetitions: Int) -> Unit
) {
    var weightInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rekord erfassen – $exerciseName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = {
                        weightInput = it
                        localError = null
                    },
                    label = { Text("Gewicht (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = {
                        repsInput = it
                        localError = null
                    },
                    label = { Text("Maximale Wiederholungen") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val shownError = localError ?: errorMessage
                if (!shownError.isNullOrBlank()) {
                    Text(
                        text = shownError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightInput.replace(',', '.').toDoubleOrNull()
                    val reps = repsInput.toIntOrNull()
                    when {
                        weight == null || weight <= 0.0 ->
                            localError = "Bitte ein gültiges Gewicht > 0 kg eingeben."
                        reps == null || reps <= 0 ->
                            localError = "Bitte gültige Wiederholungen > 0 eingeben."
                        else -> onConfirm(weight, reps)
                    }
                },
                enabled = !isSubmitting
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Abbrechen")
            }
        }
    )
}
