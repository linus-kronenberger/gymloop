package com.example.gymloop.domain.repository

import com.example.gymloop.domain.model.Exercise
import com.example.gymloop.domain.model.ExerciseId
import com.example.gymloop.domain.model.PersonalRecord

interface ExerciseRepository {
    suspend fun saveExercise(exercise: Exercise): Exercise
    suspend fun findExerciseById(id: ExerciseId): Exercise?
    suspend fun findAllExercises(): List<Exercise>
    suspend fun deleteExercise(id: ExerciseId): Boolean

    suspend fun addPersonalRecord(record: PersonalRecord): PersonalRecord
    suspend fun findRecordsForExercise(exerciseId: ExerciseId): List<PersonalRecord>
}
