package com.example.gymloop.application

import com.example.gymloop.domain.DomainError
import com.example.gymloop.domain.DomainException
import com.example.gymloop.domain.Exercise
import com.example.gymloop.domain.ExerciseId
import com.example.gymloop.domain.ExerciseName
import com.example.gymloop.domain.ExerciseRepository
import com.example.gymloop.domain.Repetitions
import com.example.gymloop.domain.WeightKg
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ListExercisesUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(): List<Exercise> = repository.listExercises()
}

class GetExerciseUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(id: ExerciseId): Exercise =
        repository.getExercise(id) ?: throw DomainException(DomainError.ExerciseNotFound(id))
}

class CreateExerciseUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(rawName: String): Exercise {
        val name = ExerciseName.of(rawName)
        return repository.createExercise(name)
    }
}

class AddPersonalRecordUseCase(private val repository: ExerciseRepository) {
    suspend operator fun invoke(
        exerciseId: ExerciseId,
        weightKg: Double,
        repetitions: Int,
        recordedAt: Instant = Clock.System.now()
    ): Exercise {
        val validWeight = WeightKg(weightKg)
        val validReps = Repetitions(repetitions)
        return repository.addRecord(
            exerciseId = exerciseId,
            weightKg = validWeight,
            repetitions = validReps,
            recordedAt = recordedAt
        )
    }
}
