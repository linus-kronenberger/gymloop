package com.example.gymloop.infrastructure.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ExercisesTable : Table("exercises") {
    val id = varchar("id", 64)
    val name = varchar("name", 80).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

object PersonalRecordsTable : Table("personal_records") {
    val id = varchar("id", 64)
    val exerciseId = varchar("exercise_id", 64)
        .references(ExercisesTable.id, onDelete = ReferenceOption.CASCADE)
    val weightKg = double("weight_kg")
    val repetitions = integer("repetitions")
    val recordedAt = timestamp("recorded_at")

    override val primaryKey = PrimaryKey(id)
}
