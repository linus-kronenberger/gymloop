package com.example.gymloop.ui

import com.example.gymloop.domain.DomainError
import com.example.gymloop.domain.DomainException
import com.example.gymloop.domain.Exercise
import com.example.gymloop.domain.ExerciseId
import com.example.gymloop.domain.ExerciseName
import com.example.gymloop.domain.ExerciseRepository
import com.example.gymloop.domain.PersonalRecord
import com.example.gymloop.domain.ProgressTrend
import com.example.gymloop.domain.RecordId
import com.example.gymloop.domain.Repetitions
import com.example.gymloop.domain.WeightKg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeExerciseRepository : ExerciseRepository {
    private val store = mutableListOf<Exercise>()
    private var nextId = 1

    override suspend fun listExercises(): List<Exercise> = store.toList()

    override suspend fun getExercise(id: ExerciseId): Exercise? =
        store.firstOrNull { it.id == id }

    override suspend fun createExercise(name: ExerciseName): Exercise {
        val created = Exercise(ExerciseId("ex-${nextId++}"), name)
        store += created
        return created
    }

    override suspend fun addRecord(
        exerciseId: ExerciseId,
        weightKg: WeightKg,
        repetitions: Repetitions,
        recordedAt: Instant
    ): Exercise {
        val idx = store.indexOfFirst { it.id == exerciseId }
        if (idx < 0) throw DomainException(DomainError.ExerciseNotFound(exerciseId))
        val updated = store[idx].addRecord(
            PersonalRecord(
                id = RecordId("rec-${nextId++}"),
                weightKg = weightKg,
                repetitions = repetitions,
                recordedAt = recordedAt
            )
        )
        store[idx] = updated
        return updated
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GymLoopViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `creating exercise and adding records updates ui state and trend`() = runTest(testDispatcher) {
        val vm = GymLoopViewModel(FakeExerciseRepository())
        advanceUntilIdle()

        val initial = assertIs<GymLoopUiState.Content>(vm.uiState.value)
        assertEquals(0, initial.exercises.size)

        vm.createExercise("Bankdrücken")
        advanceUntilIdle()

        val afterCreate = assertIs<GymLoopUiState.Content>(vm.uiState.value)
        assertEquals(1, afterCreate.exercises.size)
        val exerciseId = afterCreate.selectedExerciseId!!

        vm.addRecord(exerciseId, 80.0, 8)
        advanceUntilIdle()
        vm.addRecord(exerciseId, 85.0, 8)
        advanceUntilIdle()

        val afterRecords = assertIs<GymLoopUiState.Content>(vm.uiState.value)
        val exercise = afterRecords.selectedExercise!!
        assertEquals(2, exercise.records.size)
        assertEquals(ProgressTrend.BETTER, exercise.latestTrend())
    }
}
