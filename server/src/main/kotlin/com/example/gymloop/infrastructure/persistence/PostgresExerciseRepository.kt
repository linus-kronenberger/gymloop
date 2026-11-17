package com.example.gymloop.infrastructure.persistence

import com.example.gymloop.domain.DomainError
import com.example.gymloop.domain.DomainException
import com.example.gymloop.domain.Exercise
import com.example.gymloop.domain.ExerciseId
import com.example.gymloop.domain.ExerciseName
import com.example.gymloop.domain.ExerciseRepository
import com.example.gymloop.domain.PersonalRecord
import com.example.gymloop.domain.RecordId
import com.example.gymloop.domain.Repetitions
import com.example.gymloop.domain.WeightKg
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class PostgresExerciseRepository(private val database: Database) : ExerciseRepository {

    override suspend fun listExercises(): List<Exercise> = dbQuery {
        val exercises = ExercisesTable.selectAll()
            .orderBy(ExercisesTable.name to SortOrder.ASC)
            .map { row ->
                ExerciseId(row[ExercisesTable.id]) to ExerciseName.of(row[ExercisesTable.name])
            }

        val recordsByExercise = PersonalRecordsTable.selectAll()
            .orderBy(PersonalRecordsTable.recordedAt to SortOrder.ASC)
            .groupBy { row -> ExerciseId(row[PersonalRecordsTable.exerciseId]) }

        exercises.map { (id, name) ->
            val records = recordsByExercise[id].orEmpty().map { row ->
                PersonalRecord(
                    id = RecordId(row[PersonalRecordsTable.id]),
                    weightKg = WeightKg(row[PersonalRecordsTable.weightKg]),
                    repetitions = Repetitions(row[PersonalRecordsTable.repetitions]),
                    recordedAt = row[PersonalRecordsTable.recordedAt]
                )
            }
            Exercise(id = id, name = name, records = records)
        }
    }

    override suspend fun getExercise(id: ExerciseId): Exercise? = dbQuery {
        loadExercise(id)
    }

    override suspend fun createExercise(name: ExerciseName): Exercise = dbQuery {
        val exists = ExercisesTable.selectAll()
            .where { ExercisesTable.name eq name.value }
            .any()
        if (exists) {
            throw DomainException(DomainError.DuplicateExerciseName(name))
        }

        val id = ExerciseId(UUID.randomUUID().toString())
        ExercisesTable.insert {
            it[ExercisesTable.id] = id.value
            it[ExercisesTable.name] = name.value
        }
        Exercise(id = id, name = name, records = emptyList())
    }

    override suspend fun addRecord(
        exerciseId: ExerciseId,
        weightKg: WeightKg,
        repetitions: Repetitions,
        recordedAt: Instant
    ): Exercise = dbQuery {
        val existing = loadExercise(exerciseId)
            ?: throw DomainException(DomainError.ExerciseNotFound(exerciseId))

        val recordId = RecordId(UUID.randomUUID().toString())
        PersonalRecordsTable.insert {
            it[PersonalRecordsTable.id] = recordId.value
            it[PersonalRecordsTable.exerciseId] = exerciseId.value
            it[PersonalRecordsTable.weightKg] = weightKg.value
            it[PersonalRecordsTable.repetitions] = repetitions.value
            it[PersonalRecordsTable.recordedAt] = recordedAt
        }

        val newRecord = PersonalRecord(
            id = recordId,
            weightKg = weightKg,
            repetitions = repetitions,
            recordedAt = recordedAt
        )
        existing.addRecord(newRecord)
    }

    private fun loadExercise(id: ExerciseId): Exercise? {
        val exerciseRow = ExercisesTable.selectAll()
            .where { ExercisesTable.id eq id.value }
            .singleOrNull() ?: return null

        val records = PersonalRecordsTable.selectAll()
            .where { PersonalRecordsTable.exerciseId eq id.value }
            .orderBy(PersonalRecordsTable.recordedAt to SortOrder.ASC)
            .map { row ->
                PersonalRecord(
                    id = RecordId(row[PersonalRecordsTable.id]),
                    weightKg = WeightKg(row[PersonalRecordsTable.weightKg]),
                    repetitions = Repetitions(row[PersonalRecordsTable.repetitions]),
                    recordedAt = row[PersonalRecordsTable.recordedAt]
                )
            }

        return Exercise(
            id = ExerciseId(exerciseRow[ExercisesTable.id]),
            name = ExerciseName.of(exerciseRow[ExercisesTable.name]),
            records = records
        )
    }

    private suspend fun <T> dbQuery(block: () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
