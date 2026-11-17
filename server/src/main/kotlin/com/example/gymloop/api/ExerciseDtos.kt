package com.example.gymloop.api

import com.example.gymloop.domain.Exercise
import com.example.gymloop.domain.PersonalRecord
import kotlinx.serialization.Serializable

@Serializable
data class PersonalRecordDto(
    val id: String,
    val weightKg: Double,
    val repetitions: Int,
    val volume: Double,
    val recordedAt: String
)

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val latestTrend: String,
    val records: List<PersonalRecordDto>
)

@Serializable
data class CreateExerciseRequest(
    val name: String
)

@Serializable
data class AddRecordRequest(
    val weightKg: Double,
    val repetitions: Int,
    val recordedAt: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String
)

fun PersonalRecord.toDto(): PersonalRecordDto = PersonalRecordDto(
    id = id.value,
    weightKg = weightKg.value,
    repetitions = repetitions.value,
    volume = volume.value,
    recordedAt = recordedAt.toString()
)

fun Exercise.toDto(): ExerciseDto = ExerciseDto(
    id = id.value,
    name = name.value,
    latestTrend = latestTrend().name,
    records = sortedRecords.map { it.toDto() }
)
