package com.example.gymloop.domain

import kotlin.jvm.JvmInline

@JvmInline
value class ExerciseId(val value: String) {
    init {
        if (value.isBlank()) {
            throw DomainException(DomainError.InvalidIdentifier("ExerciseId must not be blank."))
        }
    }
}

@JvmInline
value class RecordId(val value: String) {
    init {
        if (value.isBlank()) {
            throw DomainException(DomainError.InvalidIdentifier("RecordId must not be blank."))
        }
    }
}

@JvmInline
value class ExerciseName private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH: Int = 80

        fun of(raw: String): ExerciseName {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                throw DomainException(
                    DomainError.InvalidExerciseName("Exercise name must not be empty.")
                )
            }
            if (trimmed.length > MAX_LENGTH) {
                throw DomainException(
                    DomainError.InvalidExerciseName("Exercise name must be at most $MAX_LENGTH characters.")
                )
            }
            return ExerciseName(trimmed)
        }
    }
}

@JvmInline
value class WeightKg(val value: Double) {
    init {
        if (!value.isFinite() || value <= 0.0) {
            throw DomainException(
                DomainError.InvalidWeight("Weight in kg must be greater than 0.")
            )
        }
    }

    operator fun times(repetitions: Repetitions): Volume = Volume(value * repetitions.value)
}

@JvmInline
value class Repetitions(val value: Int) {
    init {
        if (value <= 0) {
            throw DomainException(
                DomainError.InvalidRepetitions("Repetitions must be greater than 0.")
            )
        }
    }
}

@JvmInline
value class Volume(val value: Double) : Comparable<Volume> {
    override fun compareTo(other: Volume): Int = value.compareTo(other.value)
}

/** Performance trend comparing the latest personal record volume against the previous one. */
enum class ProgressTrend {
    BETTER,
    SAME,
    WORSE,
    INSUFFICIENT_DATA
}
