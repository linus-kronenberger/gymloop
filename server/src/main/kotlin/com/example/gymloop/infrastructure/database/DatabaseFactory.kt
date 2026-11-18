package com.example.gymloop.infrastructure.database

import com.example.gymloop.infrastructure.database.tables.ExercisesTable
import com.example.gymloop.infrastructure.database.tables.PersonalRecordsTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun init(
        jdbcUrl: String = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/gymloop",
        driverClassName: String = System.getenv("DATABASE_DRIVER") ?: "org.postgresql.Driver",
        user: String = System.getenv("DATABASE_USER") ?: "gymloop",
        password: String = System.getenv("DATABASE_PASSWORD") ?: "gymloop_password"
    ): Database {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.driverClassName = driverClassName
            this.username = user
            this.password = password
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val dataSource = HikariDataSource(config)
        val database = Database.connect(dataSource)

        transaction(database) {
            SchemaUtils.create(ExercisesTable, PersonalRecordsTable)
        }

        return database
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            suspendTransaction {
                block()
            }
        }
}
