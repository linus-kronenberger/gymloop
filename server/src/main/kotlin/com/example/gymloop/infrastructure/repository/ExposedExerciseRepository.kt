package com.example.gymloop.infrastructure.repository

import com.example.gymloop.domain.model.Exercise
import com.example.gymloop.domain.model.ExerciseId
import com.example.gymloop.domain.model.PersonalRecord
import com.example.gymloop.domain.repository.ExerciseRepository
import com.example.gymloop.infrastructure.database.DatabaseFactory.dbQuery
import com.example.gymloop.infrastructure.database.entities.ExerciseEntity
import com.example.gymloop.infrastructure.database.entities.PersonalRecordEntity
import com.example.gymloop.infrastructure.database.tables.ExercisesTable
import com.example.gymloop.infrastructure.database.tables.PersonalRecordsTable
import kotlin.time.Clock
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ExposedExerciseRepository : ExerciseRepository {

    override suspend fun saveExercise(exercise: Exercise): Exercise = dbQuery {
        val existing = ExerciseEntity.findById(exercise.id.value)
        if (existing != null) {
            existing.name = exercise.name.value
            existing.toDomain()
        } else {
            ExerciseEntity.new(exercise.id.value) {
                this.name = exercise.name.value
                this.createdAt = exercise.latestRecord?.recordedAt ?: Clock.System.now()
            }.toDomain()
        }
    }

    override suspend fun findExerciseById(id: ExerciseId): Exercise? = dbQuery {
        ExerciseEntity.findById(id.value)?.toDomain()
    }

    override suspend fun findAllExercises(): List<Exercise> = dbQuery {
        ExerciseEntity.all().map { it.toDomain() }
    }

    override suspend fun deleteExercise(id: ExerciseId): Boolean = dbQuery {
        val existing = ExerciseEntity.findById(id.value) ?: return@dbQuery false
        existing.delete()
        true
    }

    override suspend fun addPersonalRecord(record: PersonalRecord): PersonalRecord = dbQuery {
        val exerciseEntity = ExerciseEntity.findById(record.exerciseId.value)
            ?: throw IllegalArgumentException("Exercise with id ${record.exerciseId.value} not found")

        PersonalRecordEntity.new(record.id) {
            this.exercise = exerciseEntity
            this.weightKg = record.weight.value
            this.repetitions = record.repetitions.value
            this.recordedAt = record.recordedAt
        }.toDomain()
    }

    override suspend fun findRecordsForExercise(exerciseId: ExerciseId): List<PersonalRecord> = dbQuery {
        val entityId = EntityID(exerciseId.value, ExercisesTable)
        PersonalRecordEntity.find { PersonalRecordsTable.exerciseId eq entityId }
            .sortedBy { it.recordedAt }
            .map { it.toDomain() }
    }
}
