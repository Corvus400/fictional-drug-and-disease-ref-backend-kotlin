package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.ktor.server.application.Application
import io.ktor.server.application.log
import java.util.UUID

data class SecurityConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtRealm: String,
    val rateLimitLimit: Int,
    val rateLimitRefillSeconds: Long,
    val corsAllowedOrigins: List<String>,
    val adminHost: String,
    val adminPort: Int,
    val adminCorsAllowedOrigins: List<String>,
    val adminTokenTtlSeconds: Long,
)

fun Application.loadSecurityConfig(appEnv: String): SecurityConfig =
    SecurityConfig(
        jwtSecret = resolveJwtSecret(appEnv),
        jwtIssuer = resolveConfig("JWT_ISSUER", "security.jwtIssuer", default = "http://localhost:18080"),
        jwtAudience = resolveConfig("JWT_AUDIENCE", "security.jwtAudience", default = "fictional-drug-ref"),
        jwtRealm = resolveConfig("JWT_REALM", "security.jwtRealm", default = "fictional-drug-ref"),
        rateLimitLimit = resolveConfig("RATE_LIMIT_LIMIT", "security.rateLimitLimit", default = "60").toInt(),
        rateLimitRefillSeconds = resolveConfig(
            "RATE_LIMIT_REFILL_SECONDS",
            "security.rateLimitRefillSeconds",
            default = "60",
        ).toLong(),
        corsAllowedOrigins = resolveConfig("CORS_ALLOWED_ORIGINS", "security.corsAllowedOrigins", default = "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() },
        adminHost = resolveConfig("ADMIN_HOST", "security.adminHost", default = "127.0.0.1"),
        adminPort = resolveConfig("ADMIN_PORT", "security.adminPort", default = "19090").toInt(),
        adminCorsAllowedOrigins = resolveConfig(
            "ADMIN_CORS_ALLOWED_ORIGINS",
            "security.adminCorsAllowedOrigins",
            default = "",
        )
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() },
        adminTokenTtlSeconds = resolveConfig(
            "ADMIN_TOKEN_TTL_SECONDS",
            "security.adminTokenTtlSeconds",
            default = "3600",
        ).toLong(),
    )

private fun Application.resolveJwtSecret(appEnv: String): String =
    System.getenv("JWT_SECRET")
        ?: environment.config.propertyOrNull("security.jwtSecret")?.getString()
        ?: if (appEnv == "local") {
            UUID.randomUUID().toString().also {
                log.warn("JWT_SECRET unset; using ephemeral dev secret; tokens will not survive restart")
            }
        } else {
            error("JWT_SECRET is required when APP_ENV != local")
        }
