package com.example.gymloop.domain

import com.example.gymloop.domain.model.Exercise
import com.example.gymloop.domain.model.ExerciseId
import com.example.gymloop.domain.model.ExerciseName
import com.example.gymloop.domain.model.PersonalRecord
import com.example.gymloop.domain.model.ProgressTrend
import com.example.gymloop.domain.model.Repetitions
import com.example.gymloop.domain.model.WeightKg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExerciseDomainTest {

    @Test
    fun testWeightValidation() {
        assertFailsWith<IllegalArgumentException> {
            WeightKg(0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            WeightKg(-5.0)
        }
        val valid = WeightKg(100.0)
        assertEquals(100.0, valid.value)
    }

    @Test
    fun testRepetitionsValidation() {
        assertFailsWith<IllegalArgumentException> {
            Repetitions(0)
        }
        val valid = Repetitions(10)
        assertEquals(10, valid.value)
    }

    @Test
    fun testExerciseNameValidation() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseName("")
        }
        assertFailsWith<IllegalArgumentException> {
            ExerciseName(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            ExerciseName("A")
        }
        val valid = ExerciseName("Bench Press")
        assertEquals("Bench Press", valid.value)
    }

    @Test
    fun testProgressTrendCalculation() {
        val exerciseId = ExerciseId("ex-1")
        val exerciseName = ExerciseName("Squat")

        val t1 = Instant.fromEpochMilliseconds(1000)
        val t2 = Instant.fromEpochMilliseconds(2000)

        // Single record -> no trend
        val r1 = PersonalRecord("r1", exerciseId, WeightKg(100.0), Repetitions(5), t1)
        val exSingle = Exercise(exerciseId, exerciseName, listOf(r1))
        assertNull(exSingle.progressTrend)

        // Improved performance -> BETTER
        val rBetter = PersonalRecord("r2", exerciseId, WeightKg(105.0), Repetitions(5), t2)
        val exBetter = Exercise(exerciseId, exerciseName, listOf(r1, rBetter))
        assertEquals(ProgressTrend.BETTER, exBetter.progressTrend)

        // Worse performance -> WORSE
        val rWorse = PersonalRecord("r3", exerciseId, WeightKg(90.0), Repetitions(5), t2)
        val exWorse = Exercise(exerciseId, exerciseName, listOf(r1, rWorse))
        assertEquals(ProgressTrend.WORSE, exWorse.progressTrend)

        // Same performance -> SAME
        val rSame = PersonalRecord("r4", exerciseId, WeightKg(100.0), Repetitions(5), t2)
        val exSame = Exercise(exerciseId, exerciseName, listOf(r1, rSame))
        assertEquals(ProgressTrend.SAME, exSame.progressTrend)
    }

    @Test
    fun testEstimatedOneRepMaxEpleyFormula() {
        val exerciseId = ExerciseId("ex-1")
        val t = Instant.fromEpochMilliseconds(1000)

        // 1 rep of 100kg = 100kg
        val r1 = PersonalRecord("r1", exerciseId, WeightKg(100.0), Repetitions(1), t)
        assertEquals(100.0, r1.estimatedOneRepMaxKg)

        // 10 reps of 100kg = 100 * (1 + 10/30) = 133.33kg
        val r10 = PersonalRecord("r2", exerciseId, WeightKg(100.0), Repetitions(10), t)
        assertTrue(r10.estimatedOneRepMaxKg > 133.3 && r10.estimatedOneRepMaxKg < 133.4)
    }
}
