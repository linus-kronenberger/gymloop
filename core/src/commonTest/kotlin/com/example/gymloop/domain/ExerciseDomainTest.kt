package com.example.gymloop.domain

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExerciseDomainTest {

    @Test
    fun `value objects reject invalid inputs`() {
        assertFailsWith<DomainException> { ExerciseName.of("   ") }
        assertFailsWith<DomainException> { ExerciseName.of("a".repeat(81)) }
        assertFailsWith<DomainException> { WeightKg(0.0) }
        assertFailsWith<DomainException> { WeightKg(-10.0) }
        assertFailsWith<DomainException> { WeightKg(Double.NaN) }
        assertFailsWith<DomainException> { Repetitions(0) }
        assertFailsWith<DomainException> { Repetitions(-3) }
        assertFailsWith<DomainException> { ExerciseId("  ") }
    }

    @Test
    fun `exercise name trims whitespace`() {
        val name = ExerciseName.of("  Bankdrücken  ")
        assertEquals("Bankdrücken", name.value)
    }

    @Test
    fun `trend is insufficient data when fewer than two records exist`() {
        val empty = Exercise(ExerciseId("ex-1"), ExerciseName.of("Kniebeuge"))
        assertEquals(ProgressTrend.INSUFFICIENT_DATA, empty.latestTrend())

        val single = empty.addRecord(
            PersonalRecord(
                id = RecordId("r-1"),
                weightKg = WeightKg(100.0),
                repetitions = Repetitions(5),
                recordedAt = Instant.parse("2026-01-01T10:00:00Z")
            )
        )
        assertEquals(ProgressTrend.INSUFFICIENT_DATA, single.latestTrend())
    }

    @Test
    fun `trend reflects training volume changes chronologically`() {
        val t1 = Instant.parse("2026-01-01T10:00:00Z")
        val t2 = Instant.parse("2026-01-05T10:00:00Z")
        val t3 = Instant.parse("2026-01-10T10:00:00Z")
        val t4 = Instant.parse("2026-01-15T10:00:00Z")

        val exercise = Exercise(
            id = ExerciseId("ex-1"),
            name = ExerciseName.of("Kreuzheben"),
            records = listOf(
                // Added out of order to verify chronological sorting
                PersonalRecord(RecordId("r-2"), WeightKg(100.0), Repetitions(6), t2), // 600 -> BETTER
                PersonalRecord(RecordId("r-1"), WeightKg(100.0), Repetitions(5), t1)  // 500
            )
        )
        assertEquals(ProgressTrend.BETTER, exercise.latestTrend())

        val sameVolume = exercise.addRecord(
            PersonalRecord(RecordId("r-3"), WeightKg(120.0), Repetitions(5), t3) // 600 -> SAME
        )
        assertEquals(ProgressTrend.SAME, sameVolume.latestTrend())

        val worseVolume = sameVolume.addRecord(
            PersonalRecord(RecordId("r-4"), WeightKg(110.0), Repetitions(5), t4) // 550 -> WORSE
        )
        assertEquals(ProgressTrend.WORSE, worseVolume.latestTrend())
        assertEquals(600.0, worseVolume.bestRecordByVolume()?.volume?.value)
    }
}
