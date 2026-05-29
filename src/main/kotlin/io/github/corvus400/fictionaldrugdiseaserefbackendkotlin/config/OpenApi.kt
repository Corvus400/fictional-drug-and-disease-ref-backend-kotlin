package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.Disclaimer
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.github.smiley4.ktoropenapi.config.ExampleEncoder
import io.github.smiley4.ktoropenapi.config.OpenApiPluginConfig
import io.github.smiley4.ktoropenapi.config.SchemaGenerator
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorredoc.redoc
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/** 管理 API 専用 OpenAPI spec の識別子。public ("api") spec と分離し admin ポートでのみ提供する。 */
const val ADMIN_SPEC_NAME: String = "admin"

/** 管理 API 用の Bearer (JWT) セキュリティスキーム名。admin spec と各 admin ルートで共有する。 */
const val ADMIN_SECURITY_SCHEME: String = "AdminBearer"

private const val ADMIN_PATH_PREFIX = "/v1/admin"

fun Application.configureOpenAPI() {
    val json = AppJson

    install(OpenApi) {
        // pathFilter はルート収集時にグローバルで 1 回だけ適用される (per-spec pathFilter は無効)。
        // /v1/admin を収集対象に含めないと admin spec が空になるため、ここでは除外しない。
        // public ("api") と admin の分離は specAssigner + 各ルートの specName で行う。
        specAssigner = { path, _ ->
            if (path.startsWith(ADMIN_PATH_PREFIX)) ADMIN_SPEC_NAME else OpenApiPluginConfig.DEFAULT_SPEC_ID
        }

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

        // 管理 API 専用 spec。public spec の info/tags を汚さないよう admin 固有設定をここに閉じる。
        spec(ADMIN_SPEC_NAME) {
            info {
                title = "架空医薬品・疾病 リファレンス 管理 API"
                version = "1.0.0"
                description = "CMS 連携用の管理 API。admin コネクタ (ローカル限定) でのみ提供する。"
            }
            security {
                securityScheme(ADMIN_SECURITY_SCHEME) {
                    type = AuthType.HTTP
                    scheme = AuthScheme.BEARER
                    bearerFormat = "JWT"
                }
            }
            tags {
                tag("Admin") { description = "管理 API (CMS 連携・ローカル限定)" }
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
