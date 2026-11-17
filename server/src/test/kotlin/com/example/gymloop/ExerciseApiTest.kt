package com.example.gymloop

import com.example.gymloop.api.AddRecordRequest
import com.example.gymloop.api.CreateExerciseRequest
import com.example.gymloop.api.ExerciseDto
import com.example.gymloop.domain.ProgressTrend
import com.example.gymloop.infrastructure.persistence.DatabaseFactory
import com.example.gymloop.infrastructure.persistence.PostgresExerciseRepository
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseApiTest {

    private fun newTestRepository(): PostgresExerciseRepository {
        val db = DatabaseFactory.init(
            jdbcUrl = "jdbc:h2:mem:gymloop_${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            user = "sa",
            password = "",
            driverClassName = "org.h2.Driver"
        )
        return PostgresExerciseRepository(db)
    }

    @Test
    fun `create exercise, add records and compute volume trend`() = testApplication {
        val repository = newTestRepository()
        application {
            module(repository)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val createResp = client.post("/api/v1/exercises") {
            contentType(ContentType.Application.Json)
            setBody(CreateExerciseRequest("Bankdrücken"))
        }
        assertEquals(HttpStatusCode.Created, createResp.status)
        val created = createResp.body<ExerciseDto>()
        assertEquals("Bankdrücken", created.name)
        assertEquals(ProgressTrend.INSUFFICIENT_DATA.name, created.latestTrend)

        val r1Resp = client.post("/api/v1/exercises/${created.id}/records") {
            contentType(ContentType.Application.Json)
            setBody(AddRecordRequest(weightKg = 80.0, repetitions = 8, recordedAt = "2026-01-01T10:00:00Z"))
        }
        assertEquals(HttpStatusCode.Created, r1Resp.status)

        val r2Resp = client.post("/api/v1/exercises/${created.id}/records") {
            contentType(ContentType.Application.Json)
            setBody(AddRecordRequest(weightKg = 85.0, repetitions = 8, recordedAt = "2026-01-08T10:00:00Z"))
        }
        assertEquals(HttpStatusCode.Created, r2Resp.status)
        val afterSecond = r2Resp.body<ExerciseDto>()
        assertEquals(ProgressTrend.BETTER.name, afterSecond.latestTrend)
        assertEquals(2, afterSecond.records.size)

        val listResp = client.get("/api/v1/exercises")
        assertEquals(HttpStatusCode.OK, listResp.status)
        val all = listResp.body<List<ExerciseDto>>()
        assertEquals(1, all.size)
        assertEquals(680.0, all.first().records.last().volume)
    }

    @Test
    fun `invalid weight returns 400 and unknown exercise returns 404`() = testApplication {
        val repository = newTestRepository()
        application {
            module(repository)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val notFound = client.get("/api/v1/exercises/unknown-id")
        assertEquals(HttpStatusCode.NotFound, notFound.status)

        val created = client.post("/api/v1/exercises") {
            contentType(ContentType.Application.Json)
            setBody(CreateExerciseRequest("Kniebeuge"))
        }.body<ExerciseDto>()

        val badWeight = client.post("/api/v1/exercises/${created.id}/records") {
            contentType(ContentType.Application.Json)
            setBody(AddRecordRequest(weightKg = -5.0, repetitions = 5))
        }
        assertEquals(HttpStatusCode.BadRequest, badWeight.status)
    }
}
