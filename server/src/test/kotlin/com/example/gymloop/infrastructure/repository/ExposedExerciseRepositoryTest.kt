package com.example.gymloop.infrastructure.repository

import com.example.gymloop.domain.model.Exercise
import com.example.gymloop.domain.model.ExerciseId
import com.example.gymloop.domain.model.ExerciseName
import com.example.gymloop.domain.model.PersonalRecord
import com.example.gymloop.domain.model.ProgressTrend
import com.example.gymloop.domain.model.Repetitions
import com.example.gymloop.domain.model.WeightKg
import com.example.gymloop.infrastructure.database.DatabaseFactory
import com.example.gymloop.infrastructure.database.tables.ExercisesTable
import com.example.gymloop.infrastructure.database.tables.PersonalRecordsTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExposedExerciseRepositoryTest {

    private val repository = ExposedExerciseRepository()

    @Before
    fun setUp() {
        DatabaseFactory.init(
            jdbcUrl = "jdbc:h2:mem:gymloop_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            driverClassName = "org.h2.Driver",
            user = "sa",
            password = ""
        )
    }

    @After
    fun tearDown() {
        transaction {
            SchemaUtils.drop(PersonalRecordsTable, ExercisesTable)
        }
    }

    @Test
    fun testSaveAndFindExercise() = runBlocking {
        val exerciseId = ExerciseId("bench-press")
        val exercise = Exercise(
            id = exerciseId,
            name = ExerciseName("Bench Press")
        )

        val saved = repository.saveExercise(exercise)
        assertEquals("bench-press", saved.id.value)
        assertEquals("Bench Press", saved.name.value)

        val found = repository.findExerciseById(exerciseId)
        assertNotNull(found)
        assertEquals("Bench Press", found.name.value)
    }

    @Test
    fun testUpdateExercise() = runBlocking {
        val exerciseId = ExerciseId("squat")
        repository.saveExercise(Exercise(exerciseId, ExerciseName("Squats")))

        val updated = repository.saveExercise(Exercise(exerciseId, ExerciseName("Barbell Back Squat")))
        assertEquals("Barbell Back Squat", updated.name.value)

        val found = repository.findExerciseById(exerciseId)
        assertNotNull(found)
        assertEquals("Barbell Back Squat", found.name.value)
    }

    @Test
    fun testAddAndRetrievePersonalRecordsWithTrend() = runBlocking {
        val exerciseId = ExerciseId("deadlift")
        repository.saveExercise(Exercise(exerciseId, ExerciseName("Conventional Deadlift")))

        val t1 = Instant.fromEpochMilliseconds(1_000_000)
        val t2 = Instant.fromEpochMilliseconds(2_000_000)

        val r1 = PersonalRecord("r1", exerciseId, WeightKg(140.0), Repetitions(5), t1)
        val r2 = PersonalRecord("r2", exerciseId, WeightKg(150.0), Repetitions(5), t2)

        repository.addPersonalRecord(r1)
        repository.addPersonalRecord(r2)

        val records = repository.findRecordsForExercise(exerciseId)
        assertEquals(2, records.size)
        assertEquals(140.0, records[0].weight.value)
        assertEquals(150.0, records[1].weight.value)

        val exerciseWithRecords = repository.findExerciseById(exerciseId)
        assertNotNull(exerciseWithRecords)
        assertEquals(2, exerciseWithRecords.records.size)
        assertEquals(ProgressTrend.BETTER, exerciseWithRecords.progressTrend)
    }

    @Test
    fun testDeleteExerciseCascadesRecords() = runBlocking {
        val exerciseId = ExerciseId("overhead-press")
        repository.saveExercise(Exercise(exerciseId, ExerciseName("Overhead Press")))

        val record = PersonalRecord("ohp-1", exerciseId, WeightKg(60.0), Repetitions(5), Instant.fromEpochMilliseconds(1000))
        repository.addPersonalRecord(record)

        val deleted = repository.deleteExercise(exerciseId)
        assertTrue(deleted)

        assertNull(repository.findExerciseById(exerciseId))
        val remainingRecords = repository.findRecordsForExercise(exerciseId)
        assertTrue(remainingRecords.isEmpty())
    }
}
