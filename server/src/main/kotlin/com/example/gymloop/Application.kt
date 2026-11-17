package com.example.gymloop

import com.example.gymloop.api.ErrorResponse
import com.example.gymloop.api.exerciseRoutes
import com.example.gymloop.application.AddPersonalRecordUseCase
import com.example.gymloop.application.CreateExerciseUseCase
import com.example.gymloop.application.GetExerciseUseCase
import com.example.gymloop.application.ListExercisesUseCase
import com.example.gymloop.domain.DomainError
import com.example.gymloop.domain.DomainException
import com.example.gymloop.domain.ExerciseRepository
import com.example.gymloop.infrastructure.persistence.DatabaseFactory
import com.example.gymloop.infrastructure.persistence.PostgresExerciseRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module(repository: ExerciseRepository? = null) {
    val activeRepository = repository ?: PostgresExerciseRepository(DatabaseFactory.init())

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }
    install(CORS) {
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        anyHost()
    }
    install(StatusPages) {
        exception<DomainException> { call, cause ->
            val status = when (cause.error) {
                is DomainError.ExerciseNotFound -> HttpStatusCode.NotFound
                is DomainError.DuplicateExerciseName -> HttpStatusCode.Conflict
                else -> HttpStatusCode.BadRequest
            }
            call.respond(status, ErrorResponse(cause.error.message))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid request"))
        }
    }

    val listExercises = ListExercisesUseCase(activeRepository)
    val getExercise = GetExerciseUseCase(activeRepository)
    val createExercise = CreateExerciseUseCase(activeRepository)
    val addPersonalRecord = AddPersonalRecordUseCase(activeRepository)

    routing {
        get("/health") {
            call.respondText("OK")
        }
        exerciseRoutes(
            listExercises = listExercises,
            getExercise = getExercise,
            createExercise = createExercise,
            addPersonalRecord = addPersonalRecord
        )
    }
}
