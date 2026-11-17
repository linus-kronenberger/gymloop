package com.example.gymloop

import com.example.gymloop.infrastructure.persistence.DatabaseFactory
import com.example.gymloop.infrastructure.persistence.PostgresExerciseRepository
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testHealth() = testApplication {
        val db = DatabaseFactory.init(
            jdbcUrl = "jdbc:h2:mem:health_${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            user = "sa",
            password = "",
            driverClassName = "org.h2.Driver"
        )
        application {
            module(PostgresExerciseRepository(db))
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }
}
