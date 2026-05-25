package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.moduleWithDatabaseDispatcher
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenApiContractTest {
    @Test
    fun `business endpoint 2xx response schemas stay compatible with mock baseline`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/openapi.json")

        assertEquals(HttpStatusCode.OK, response.status)
        val actual = OpenApiDocument.parse(response.bodyAsText())
        val baseline = OpenApiDocument.parse(Path.of("contract/mock-openapi.json").readText())

        businessPaths.forEach { path ->
            val baselineOperation = baseline.getOperation(path)
            val actualOperation = actual.getOperation(path)
            val baseline2xx = baselineOperation.successResponseCodes()

            assertTrue(baseline2xx.isNotEmpty(), "$path must define at least one 2xx baseline response")
            assertEquals(
                baseline2xx,
                actualOperation.successResponseCodes(),
                "$path 2xx response code set changed",
            )

            baseline2xx.forEach { statusCode ->
                assertEquals(
                    baselineOperation.normalizedContentSchemas(statusCode, baseline),
                    actualOperation.normalizedContentSchemas(statusCode, actual),
                    "$path $statusCode response schema changed",
                )
            }
        }

        assertStringProperty(actual, "/v1/drugs/{id}", "id")
        assertStringProperty(actual, "/v1/diseases/{id}", "id")
        assertStringProperty(actual, "/v1/drugs", "items[].id")
        assertStringProperty(actual, "/v1/diseases", "items[].id")
    }

    private fun OpenApiDocument.getOperation(path: String): JsonObject {
        val operation = paths[path]?.jsonObject?.get("get")?.jsonObject
        requireNotNull(operation) { "GET $path is missing from OpenAPI document" }
        return operation
    }

    private fun JsonObject.successResponseCodes(): Set<String> =
        getValue("responses").jsonObject.keys
            .filter { code -> code.toIntOrNull() in 200..299 }
            .toSortedSet()

    private fun JsonObject.normalizedContentSchemas(
        statusCode: String,
        document: OpenApiDocument,
    ): JsonObject {
        val response = getValue("responses").jsonObject.getValue(statusCode).jsonObject
        val content = response.getValue("content").jsonObject
        return JsonObject(
            content.toSortedMap().mapValues { (_, mediaType) ->
                val schema = mediaType.jsonObject.getValue("schema")
                schema.normalizeWithComponents(root = document)
            },
        )
    }

    private fun JsonElement.normalizeWithComponents(
        root: OpenApiDocument,
        seenRefs: Set<String> = emptySet(),
    ): JsonElement =
        when (this) {
            is JsonArray -> {
                val normalized = map { item -> item.normalizeWithComponents(root, seenRefs) }
                if (normalized.all { item -> item is kotlinx.serialization.json.JsonPrimitive }) {
                    JsonArray(normalized.sortedBy { item -> item.toString() })
                } else {
                    JsonArray(normalized)
                }
            }
            is JsonObject -> {
                get("\$ref")?.jsonPrimitive?.content?.let { ref ->
                    val schemaName = ref.removePrefix("#/components/schemas/")
                    require(ref.startsWith("#/components/schemas/")) { "Unsupported OpenAPI ref: $ref" }
                    require(schemaName !in seenRefs) { "Recursive OpenAPI schema ref detected: $ref" }
                    val resolved = root.components.getValue(schemaName)
                    return resolved.normalizeWithComponents(root, seenRefs + schemaName)
                }
                JsonObject(
                    entries
                        .asSequence()
                        .filterNot { (key, _) -> key in ignoredSchemaKeys }
                        .sortedBy { (key, _) -> key }
                        .associate { (key, value) -> key to value.normalizeWithComponents(root, seenRefs) },
                )
            }
            else -> this
        }

    private fun assertStringProperty(
        document: OpenApiDocument,
        path: String,
        propertyPath: String,
    ) {
        val response = document.getOperation(path).getValue("responses").jsonObject.getValue("200").jsonObject
        val content = response.getValue("content").jsonObject.getValue("application/json").jsonObject
        val schema = content.getValue("schema").normalizeWithComponents(document).jsonObject
        val property =
            if (propertyPath == "items[].id") {
                schema.getValue("properties").jsonObject
                    .getValue("items").jsonObject
                    .getValue("items").jsonObject
                    .getValue("properties").jsonObject
                    .getValue("id").jsonObject
            } else {
                schema.getValue("properties").jsonObject
                    .getValue(propertyPath).jsonObject
            }

        assertEquals("string", property.getValue("type").jsonPrimitive.content, "$path $propertyPath wire type")
    }

    private data class OpenApiDocument(
        val paths: JsonObject,
        val components: JsonObject,
    ) {
        companion object {
            fun parse(raw: String): OpenApiDocument {
                val root = parser.parseToJsonElement(raw).jsonObject
                return OpenApiDocument(
                    paths = root.getValue("paths").jsonObject,
                    components = root.getValue("components").jsonObject.getValue("schemas").jsonObject,
                )
            }
        }
    }

    private companion object {
        val parser: Json = Json { ignoreUnknownKeys = true }

        val businessPaths: List<String> = listOf(
            "/v1/drugs",
            "/v1/drugs/{id}",
            "/v1/diseases",
            "/v1/diseases/{id}",
            "/v1/categories",
            "/v1/images/dosage-forms/{form}",
            "/v1/images/drugs/{drugId}",
        )

        val ignoredSchemaKeys: Set<String> = setOf(
            "description",
            "example",
            "examples",
            "title",
        )
    }
}
