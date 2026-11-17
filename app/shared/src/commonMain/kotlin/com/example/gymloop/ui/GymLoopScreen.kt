package com.example.gymloop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymloop.domain.Exercise
import com.example.gymloop.domain.ExerciseId
import com.example.gymloop.domain.PersonalRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymLoopScreen(
    viewModel: GymLoopViewModel = viewModel { GymLoopViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddRecordFor by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("gymloop", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("Aktualisieren")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is GymLoopUiState.Content) {
                FloatingActionButton(onClick = {
                    viewModel.clearActionError()
                    showCreateDialog = true
                }) {
                    Text("+ Übung", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpanded = maxWidth >= 600.dp

            when (val state = uiState) {
                is GymLoopUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is GymLoopUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Erneut versuchen")
                        }
                    }
                }

                is GymLoopUiState.Content -> {
                    if (isExpanded) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            ExerciseListPane(
                                exercises = state.exercises,
                                selectedId = state.selectedExerciseId,
                                onSelect = { viewModel.selectExercise(it) },
                                modifier = Modifier
                                    .weight(0.38f)
                                    .fillMaxHeight()
                            )
                            HorizontalDivider(modifier = Modifier.fillMaxHeight().widthIn(max = 1.dp))
                            ExerciseDetailPane(
                                exercise = state.selectedExercise,
                                showBackButton = false,
                                onBack = {},
                                onAddRecordClick = {
                                    viewModel.clearActionError()
                                    showAddRecordFor = it
                                },
                                modifier = Modifier
                                    .weight(0.62f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        val selected = state.selectedExercise
                        if (selected == null) {
                            ExerciseListPane(
                                exercises = state.exercises,
                                selectedId = null,
                                onSelect = { viewModel.selectExercise(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ExerciseDetailPane(
                                exercise = selected,
                                showBackButton = true,
                                onBack = { viewModel.selectExercise(null) },
                                onAddRecordClick = {
                                    viewModel.clearActionError()
                                    showAddRecordFor = it
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (showCreateDialog) {
                        CreateExerciseDialog(
                            isSubmitting = state.isSubmitting,
                            errorMessage = state.actionError,
                            onDismiss = { showCreateDialog = false },
                            onConfirm = { rawName ->
                                viewModel.createExercise(rawName) {
                                    showCreateDialog = false
                                }
                            }
                        )
                    }

                    showAddRecordFor?.let { targetExercise ->
                        AddRecordDialog(
                            exerciseName = targetExercise.name.value,
                            isSubmitting = state.isSubmitting,
                            errorMessage = state.actionError,
                            onDismiss = { showAddRecordFor = null },
                            onConfirm = { weight, reps ->
                                viewModel.addRecord(targetExercise.id, weight, reps) {
                                    showAddRecordFor = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseListPane(
    exercises: List<Exercise>,
    selectedId: ExerciseId?,
    onSelect: (ExerciseId) -> Unit,
    modifier: Modifier = Modifier
) {
    if (exercises.isEmpty()) {
        Box(
            modifier = modifier.padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Noch keine Übungen vorhanden.\nTippe auf \"+ Übung\", um zu starten.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(exercises, key = { it.id.value }) { exercise ->
            val isSelected = exercise.id == selectedId
            val latest = exercise.latestRecord()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(exercise.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name.value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val subtitle = if (latest == null) {
                            "Noch kein Rekord"
                        } else {
                            "Letzter Rekord: ${formatNumber(latest.weightKg.value)} kg × ${latest.repetitions.value} Wdh."
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TrendBadge(trend = exercise.latestTrend())
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailPane(
    exercise: Exercise?,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onAddRecordClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier
) {
    if (exercise == null) {
        Box(
            modifier = modifier.padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Wähle eine Übung aus, um den Verlauf zu sehen.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showBackButton) {
                        TextButton(onClick = onBack) {
                            Text("← Zurück")
                        }
                    }
                    Text(
                        text = exercise.name.value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(onClick = { onAddRecordClick(exercise) }) {
                    Text("+ Rekord")
                }
            }
        }

        item {
            ProgressChart(
                records = exercise.sortedRecords,
                latestTrend = exercise.latestTrend()
            )
        }

        item {
            Text(
                text = "Rekord-Historie",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        val reversedRecords = exercise.sortedRecords.asReversed()
        items(reversedRecords, key = { it.id.value }) { record ->
            RecordHistoryRow(record = record)
        }
    }
}

@Composable
private fun RecordHistoryRow(record: PersonalRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${formatNumber(record.weightKg.value)} kg × ${record.repetitions.value} Wdh.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = record.recordedAt.toString().take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Volumen: ${formatNumber(record.volume.value)} kg",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
