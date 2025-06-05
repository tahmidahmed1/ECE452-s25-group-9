package com.example.gooddeedfeed

import com.typesafe.config.ConfigFactory
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = ConfigFactory.load().getConfig("ktor.database")
        val url = config.getString("url")
        val user = config.getString("user")
        val password = config.getString("password")
        Database.connect(url, driver = "org.postgresql.Driver", user = user, password = password)
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(UserTable)
        }
    }

    suspend fun <T> dbQuery(block: () -> T): T =
        newSuspendedTransaction {
            block()
        }
} 
