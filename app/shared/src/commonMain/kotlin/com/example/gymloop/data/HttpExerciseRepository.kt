package com.example.gymloop.data

import com.example.gymloop.domain.Exercise
import com.example.gymloop.domain.ExerciseId
import com.example.gymloop.domain.ExerciseName
import com.example.gymloop.domain.ExerciseRepository
import com.example.gymloop.domain.PersonalRecord
import com.example.gymloop.domain.RecordId
import com.example.gymloop.domain.Repetitions
import com.example.gymloop.domain.WeightKg
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class PersonalRecordWireDto(
    val id: String,
    val weightKg: Double,
    val repetitions: Int,
    val volume: Double,
    val recordedAt: String
)

@Serializable
private data class ExerciseWireDto(
    val id: String,
    val name: String,
    val latestTrend: String,
    val records: List<PersonalRecordWireDto>
)

@Serializable
private data class CreateExerciseWireRequest(val name: String)

@Serializable
private data class AddRecordWireRequest(
    val weightKg: Double,
    val repetitions: Int,
    val recordedAt: String
)

@Serializable
private data class ErrorWireResponse(val error: String)

fun createGymLoopHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }
}

class HttpExerciseRepository(
    private val client: HttpClient = createGymLoopHttpClient(),
    private val baseUrl: String = defaultApiBaseUrl()
) : ExerciseRepository {

    override suspend fun listExercises(): List<Exercise> {
        val response = client.get("$baseUrl/api/v1/exercises")
        ensureSuccess(response)
        return response.body<List<ExerciseWireDto>>().map { it.toDomain() }
    }

    override suspend fun getExercise(id: ExerciseId): Exercise? {
        val response = client.get("$baseUrl/api/v1/exercises/${id.value}")
        if (response.status == HttpStatusCode.NotFound) return null
        ensureSuccess(response)
        return response.body<ExerciseWireDto>().toDomain()
    }

    override suspend fun createExercise(name: ExerciseName): Exercise {
        val response = client.post("$baseUrl/api/v1/exercises") {
            contentType(ContentType.Application.Json)
            setBody(CreateExerciseWireRequest(name.value))
        }
        ensureSuccess(response)
        return response.body<ExerciseWireDto>().toDomain()
    }

    override suspend fun addRecord(
        exerciseId: ExerciseId,
        weightKg: WeightKg,
        repetitions: Repetitions,
        recordedAt: Instant
    ): Exercise {
        val response = client.post("$baseUrl/api/v1/exercises/${exerciseId.value}/records") {
            contentType(ContentType.Application.Json)
            setBody(
                AddRecordWireRequest(
                    weightKg = weightKg.value,
                    repetitions = repetitions.value,
                    recordedAt = recordedAt.toString()
                )
            )
        }
        ensureSuccess(response)
        return response.body<ExerciseWireDto>().toDomain()
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            val message = runCatching { response.body<ErrorWireResponse>().error }
                .getOrElse { "Server error (${response.status.value})" }
            throw IllegalStateException(message)
        }
    }

    private fun ExerciseWireDto.toDomain(): Exercise = Exercise(
        id = ExerciseId(id),
        name = ExerciseName.of(name),
        records = records.map {
            PersonalRecord(
                id = RecordId(it.id),
                weightKg = WeightKg(it.weightKg),
                repetitions = Repetitions(it.repetitions),
                recordedAt = Instant.parse(it.recordedAt)
            )
        }
    )
}
