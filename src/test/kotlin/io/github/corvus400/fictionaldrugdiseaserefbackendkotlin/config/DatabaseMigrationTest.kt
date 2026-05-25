package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseMigrationTest {
    @Test
    fun `Flyway creates drugs and diseases tables in Postgres`() {
        PostgresTestSupport.dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                    AND table_name IN ('drugs', 'diseases')
                ORDER BY table_name
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { rows ->
                    val tableNames = buildList {
                        while (rows.next()) {
                            add(rows.getString("table_name"))
                        }
                    }
                    assertEquals(listOf("diseases", "drugs"), tableNames)
                }
            }
        }
    }
}
