package com.example.gymloop.domain

import kotlinx.datetime.Instant

/**
 * Single personal record entry for an exercise, capturing weight in kg and max repetitions
 * at a specific point in time.
 */
data class PersonalRecord(
    val id: RecordId,
    val weightKg: WeightKg,
    val repetitions: Repetitions,
    val recordedAt: Instant
) {
    val volume: Volume
        get() = weightKg * repetitions
}
