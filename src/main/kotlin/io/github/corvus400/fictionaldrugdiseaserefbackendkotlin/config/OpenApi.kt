package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.Disclaimer
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.ExampleEncoder
import io.github.smiley4.ktoropenapi.config.SchemaGenerator
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorredoc.redoc
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureOpenAPI() {
    val json = AppJson

    install(OpenApi) {
        info {
            title = "架空医薬品・疾病 リファレンス Backend API"
            version = "1.0.0"
            description = buildApiDescription()
        }

        schemas {
            generator = SchemaGenerator.kotlinx(json)
        }

        examples {
            exampleEncoder = ExampleEncoder.kotlinx(json)
        }

        tags {
            ApiTag.entries.forEach { apiTag ->
                tag(apiTag.tagName) { description = apiTag.description }
            }
        }
    }

    routing {
        route("/openapi.json") {
            openApi()
        }

        route("/swagger") {
            swaggerUI("/openapi.json")
        }

        route("/redoc") {
            redoc("/openapi.json")
        }
    }
}

private fun buildApiDescription(): String {
    val categoryList = ApiTag.entries.joinToString(separator = "\n") { apiTag ->
        "- **${apiTag.tagName}**: ${apiTag.description}"
    }
    return """
        |${Disclaimer.SHORT}
        |
        |${Disclaimer.FULL_JA_EN}
        |
        |架空医薬品・疾病リファレンス Backend API は、実 DB の架空データを返す Ktor API です。
        |
        |## APIカテゴリ
        |$categoryList
    """.trimMargin()
}
