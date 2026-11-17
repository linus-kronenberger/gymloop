package com.example.gymloop.domain

/** Explicit domain errors for exercise and personal record operations. */
sealed interface DomainError {
    val message: String

    data class InvalidExerciseName(override val message: String) : DomainError
    data class InvalidWeight(override val message: String) : DomainError
    data class InvalidRepetitions(override val message: String) : DomainError
    data class InvalidIdentifier(override val message: String) : DomainError
    data class ExerciseNotFound(val exerciseId: ExerciseId) : DomainError {
        override val message: String = "Exercise '${exerciseId.value}' was not found."
    }
    data class DuplicateExerciseName(val name: ExerciseName) : DomainError {
        override val message: String = "Exercise '${name.value}' already exists."
    }
}

/** Exception carrying a typed [DomainError] across boundary layers. */
class DomainException(val error: DomainError) : IllegalArgumentException(error.message)
