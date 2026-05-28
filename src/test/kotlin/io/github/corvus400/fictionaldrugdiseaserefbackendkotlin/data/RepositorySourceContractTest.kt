package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepositorySourceContractTest {
    @Test
    fun `delete reference guards use PostgreSQL JSONB containment in the database`() {
        val dataSourcePath = Path.of(
            "src/main/kotlin/io/github/corvus400/fictionaldrugdiseaserefbackendkotlin/data",
        )
        val repositorySources = listOf(
            dataSourcePath.resolve("ExposedDrugRepository.kt").readText(),
            dataSourcePath.resolve("ExposedDiseaseRepository.kt").readText(),
            dataSourcePath.resolve("JsonbExpressions.kt").readText(),
        )
        val combinedSource = repositorySources.joinToString(separator = "\n")

        assertTrue(
            combinedSource.contains("@>"),
            "Reference delete guards must use PostgreSQL JSONB containment instead of Kotlin-side row scans.",
        )
        assertFalse(
            combinedSource.contains(".firstOrNull { row -> publicId in row["),
            "Reference delete guards must not load JSONB documents and scan related ids in Kotlin.",
        )
    }
}
