package com.example.gymloop.infrastructure.database.tables

import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object ExercisesTable : IdTable<String>("exercises") {
    override val id: Column<EntityID<String>> = varchar("id", 64).entityId()
    val name: Column<String> = varchar("name", 255)
    val createdAt: Column<Instant> = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object PersonalRecordsTable : IdTable<String>("personal_records") {
    override val id: Column<EntityID<String>> = varchar("id", 64).entityId()
    val exerciseId: Column<EntityID<String>> = reference("exercise_id", ExercisesTable, onDelete = ReferenceOption.CASCADE)
    val weightKg: Column<Double> = double("weight_kg")
    val repetitions: Column<Int> = integer("repetitions")
    val recordedAt: Column<Instant> = timestamp("recorded_at")

    override val primaryKey = PrimaryKey(id)
}
