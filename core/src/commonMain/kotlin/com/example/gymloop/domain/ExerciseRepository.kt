package com.example.gymloop.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Persistence boundary for [Exercise] aggregates. */
interface ExerciseRepository {
    suspend fun listExercises(): List<Exercise>
    suspend fun getExercise(id: ExerciseId): Exercise?
    suspend fun createExercise(name: ExerciseName): Exercise
    suspend fun addRecord(
        exerciseId: ExerciseId,
        weightKg: WeightKg,
        repetitions: Repetitions,
        recordedAt: Instant = Clock.System.now()
    ): Exercise
}
