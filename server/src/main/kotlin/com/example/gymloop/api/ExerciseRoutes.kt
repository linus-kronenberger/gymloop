package com.example.gymloop.api

import com.example.gymloop.application.AddPersonalRecordUseCase
import com.example.gymloop.application.CreateExerciseUseCase
import com.example.gymloop.application.GetExerciseUseCase
import com.example.gymloop.application.ListExercisesUseCase
import com.example.gymloop.domain.ExerciseId
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun Route.exerciseRoutes(
    listExercises: ListExercisesUseCase,
    getExercise: GetExerciseUseCase,
    createExercise: CreateExerciseUseCase,
    addPersonalRecord: AddPersonalRecordUseCase
) {
    route("/api/v1/exercises") {
        get {
            val exercises = listExercises().map { it.toDto() }
            call.respond(HttpStatusCode.OK, exercises)
        }

        post {
            val request = call.receive<CreateExerciseRequest>()
            val created = createExercise(request.name)
            call.respond(HttpStatusCode.Created, created.toDto())
        }

        get("/{id}") {
            val rawId = call.parameters["id"].orEmpty()
            val exercise = getExercise(ExerciseId(rawId))
            call.respond(HttpStatusCode.OK, exercise.toDto())
        }

        post("/{id}/records") {
            val rawId = call.parameters["id"].orEmpty()
            val request = call.receive<AddRecordRequest>()
            val recordedAt = request.recordedAt?.let { Instant.parse(it) } ?: Clock.System.now()
            val updated = addPersonalRecord(
                exerciseId = ExerciseId(rawId),
                weightKg = request.weightKg,
                repetitions = request.repetitions,
                recordedAt = recordedAt
            )
            call.respond(HttpStatusCode.Created, updated.toDto())
        }
    }
}
