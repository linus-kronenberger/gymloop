package com.example.gymloop.domain.model

import kotlin.math.roundToInt
import kotlin.time.Instant

@JvmInline
value class ExerciseId(val value: String) {
    init {
        require(value.isNotBlank()) { "ExerciseId must not be blank" }
    }
}

@JvmInline
value class ExerciseName(val value: String) {
    init {
        require(value.isNotBlank()) { "ExerciseName must not be blank" }
        require(value.length in 2..100) { "ExerciseName length must be between 2 and 100 characters" }
    }
}

@JvmInline
value class WeightKg(val value: Double) {
    init {
        require(value > 0.0) { "Weight must be greater than 0 kg" }
        require(value <= 1000.0) { "Weight must not exceed 1000 kg" }
    }
}

@JvmInline
value class Repetitions(val value: Int) {
    init {
        require(value > 0) { "Repetitions must be greater than 0" }
        require(value <= 500) { "Repetitions must not exceed 500" }
    }
}

enum class ProgressTrend {
    BETTER,
    SAME,
    WORSE
}

data class PersonalRecord(
    val id: String,
    val exerciseId: ExerciseId,
    val weight: WeightKg,
    val repetitions: Repetitions,
    val recordedAt: Instant
) {
    /**
     * Estimated 1 Rep Max using the Epley formula: weight * (1 + repetitions / 30.0)
     */
    val estimatedOneRepMaxKg: Double
        get() = if (repetitions.value == 1) {
            weight.value
        } else {
            weight.value * (1.0 + repetitions.value / 30.0)
        }

    fun comparePerformance(other: PersonalRecord): ProgressTrend {
        val diff = this.estimatedOneRepMaxKg - other.estimatedOneRepMaxKg
        val roundedDiff = (diff * 100).roundToInt() / 100.0
        return when {
            roundedDiff > 0.01 -> ProgressTrend.BETTER
            roundedDiff < -0.01 -> ProgressTrend.WORSE
            else -> ProgressTrend.SAME
        }
    }
}

data class Exercise(
    val id: ExerciseId,
    val name: ExerciseName,
    val records: List<PersonalRecord> = emptyList()
) {
    val latestRecord: PersonalRecord?
        get() = records.maxByOrNull { it.recordedAt }

    val progressTrend: ProgressTrend?
        get() {
            val sorted = records.sortedBy { it.recordedAt }
            if (sorted.size < 2) return null
            val latest = sorted.last()
            val previous = sorted[sorted.size - 2]
            return latest.comparePerformance(previous)
        }
}
