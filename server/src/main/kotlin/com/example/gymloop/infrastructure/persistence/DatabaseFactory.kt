package com.example.gymloop.infrastructure.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(
        jdbcUrl: String = System.getenv("POSTGRES_URL") ?: "jdbc:postgresql://localhost:5432/gymloop",
        user: String = System.getenv("POSTGRES_USER") ?: "gymloop",
        password: String = System.getenv("POSTGRES_PASSWORD") ?: "gymloop",
        driverClassName: String = "org.postgresql.Driver"
    ): Database {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = password
            this.driverClassName = driverClassName
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
}
