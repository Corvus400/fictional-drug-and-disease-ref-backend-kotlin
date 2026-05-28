package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.SecurityConfig
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.mintAdminToken
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class AdminTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresInSeconds: Long,
)

fun Route.adminTokenRoutes(securityConfig: SecurityConfig) {
    post("/token") {
        val token = mintAdminToken(securityConfig)
        call.respond(
            AdminTokenResponse(
                accessToken = token.accessToken,
                tokenType = "Bearer",
                expiresInSeconds = securityConfig.adminTokenTtlSeconds,
            ),
        )
    }
}
