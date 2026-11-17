package com.example.gymloop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymloop.application.AddPersonalRecordUseCase
import com.example.gymloop.application.CreateExerciseUseCase
import com.example.gymloop.application.ListExercisesUseCase
import com.example.gymloop.data.HttpExerciseRepository
import com.example.gymloop.domain.Exercise
import com.example.gymloop.domain.ExerciseId
import com.example.gymloop.domain.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GymLoopUiState {
    data object Loading : GymLoopUiState
    data class Content(
        val exercises: List<Exercise>,
        val selectedExerciseId: ExerciseId? = null,
        val actionError: String? = null,
        val isSubmitting: Boolean = false
    ) : GymLoopUiState {
        val selectedExercise: Exercise?
            get() = exercises.firstOrNull { it.id == selectedExerciseId }
    }
    data class Error(val message: String) : GymLoopUiState
}

class GymLoopViewModel(
    repository: ExerciseRepository = HttpExerciseRepository()
) : ViewModel() {

    private val listExercises = ListExercisesUseCase(repository)
    private val createExercise = CreateExerciseUseCase(repository)
    private val addPersonalRecord = AddPersonalRecordUseCase(repository)

    private val _uiState = MutableStateFlow<GymLoopUiState>(GymLoopUiState.Loading)
    val uiState: StateFlow<GymLoopUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val previousSelection = (_uiState.value as? GymLoopUiState.Content)?.selectedExerciseId
            _uiState.value = GymLoopUiState.Loading
            runCatching { listExercises() }
                .onSuccess { exercises ->
                    val selected = exercises.firstOrNull { it.id == previousSelection }?.id
                        ?: exercises.firstOrNull()?.id
                    _uiState.value = GymLoopUiState.Content(
                        exercises = exercises,
                        selectedExerciseId = selected
                    )
                }
                .onFailure { error ->
                    _uiState.value = GymLoopUiState.Error(
                        error.message ?: "Verbindung zum Server fehlgeschlagen."
                    )
                }
        }
    }

    fun selectExercise(id: ExerciseId?) {
        val current = _uiState.value as? GymLoopUiState.Content ?: return
        _uiState.value = current.copy(selectedExerciseId = id, actionError = null)
    }

    fun clearActionError() {
        val current = _uiState.value as? GymLoopUiState.Content ?: return
        _uiState.value = current.copy(actionError = null)
    }

    fun createExercise(rawName: String, onSuccess: () -> Unit = {}) {
        val current = _uiState.value as? GymLoopUiState.Content ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true, actionError = null)
            runCatching { createExercise.invoke(rawName) }
                .onSuccess { created ->
                    val updatedList = (current.exercises + created).sortedBy { it.name.value.lowercase() }
                    _uiState.value = current.copy(
                        exercises = updatedList,
                        selectedExerciseId = created.id,
                        isSubmitting = false,
                        actionError = null
                    )
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isSubmitting = false,
                        actionError = error.message ?: "Übung konnte nicht angelegt werden."
                    )
                }
        }
    }

    fun addRecord(
        exerciseId: ExerciseId,
        weightKg: Double,
        repetitions: Int,
        onSuccess: () -> Unit = {}
    ) {
        val current = _uiState.value as? GymLoopUiState.Content ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true, actionError = null)
            runCatching { addPersonalRecord.invoke(exerciseId, weightKg, repetitions) }
                .onSuccess { updatedExercise ->
                    val updatedList = current.exercises.map {
                        if (it.id == updatedExercise.id) updatedExercise else it
                    }
                    _uiState.value = current.copy(
                        exercises = updatedList,
                        selectedExerciseId = updatedExercise.id,
                        isSubmitting = false,
                        actionError = null
                    )
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isSubmitting = false,
                        actionError = error.message ?: "Rekord konnte nicht gespeichert werden."
                    )
                }
        }
    }
}
