package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.ADMIN_SPEC_NAME
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.SecurityConfig
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.mintAdminToken
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

@Serializable
data class AdminTokenResponse(
    val accessToken: String,
    val token: String,
    val tokenType: String,
    val expiresInSeconds: Long,
)

private val adminTokenDocs: RouteConfig.() -> Unit = {
    summary = "管理 API トークンを発行する"
    description = "CMS が管理 API を呼ぶための短命 JWT (admin scope) を発行する。" +
        "admin ポート到達=信頼のため認証不要 (bootstrap)。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    response {
        code(HttpStatusCode.OK) {
            description = "発行された管理トークン (accessToken と token は同値エイリアス)"
            body<AdminTokenResponse>()
        }
    }
}

fun Route.adminTokenRoutes(securityConfig: SecurityConfig) {
    post("/token", adminTokenDocs) {
        val token = mintAdminToken(securityConfig)
        call.respond(
            AdminTokenResponse(
                accessToken = token.accessToken,
                token = token.accessToken,
                tokenType = "Bearer",
                expiresInSeconds = securityConfig.adminTokenTtlSeconds,
            ),
        )
    }
}
