package com.example.gymloop.infrastructure.database.entities

import com.example.gymloop.domain.model.Exercise
import com.example.gymloop.domain.model.ExerciseId
import com.example.gymloop.domain.model.ExerciseName
import com.example.gymloop.domain.model.PersonalRecord
import com.example.gymloop.domain.model.Repetitions
import com.example.gymloop.domain.model.WeightKg
import com.example.gymloop.infrastructure.database.tables.ExercisesTable
import com.example.gymloop.infrastructure.database.tables.PersonalRecordsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

class ExerciseEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ExerciseEntity>(ExercisesTable)

    var name by ExercisesTable.name
    var createdAt by ExercisesTable.createdAt
    val records by PersonalRecordEntity referrersOn PersonalRecordsTable.exerciseId

    fun toDomain(): Exercise {
        return Exercise(
            id = ExerciseId(id.value),
            name = ExerciseName(name),
            records = records.map { it.toDomain() }
        )
    }
}

class PersonalRecordEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, PersonalRecordEntity>(PersonalRecordsTable)

    var exercise by ExerciseEntity referencedOn PersonalRecordsTable.exerciseId
    var weightKg by PersonalRecordsTable.weightKg
    var repetitions by PersonalRecordsTable.repetitions
    var recordedAt by PersonalRecordsTable.recordedAt

    fun toDomain(): PersonalRecord {
        return PersonalRecord(
            id = id.value,
            exerciseId = ExerciseId(exercise.id.value),
            weight = WeightKg(weightKg),
            repetitions = Repetitions(repetitions),
            recordedAt = recordedAt
        )
    }
}
