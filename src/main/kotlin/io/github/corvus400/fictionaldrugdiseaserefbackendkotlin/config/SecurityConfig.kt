package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.ktor.server.application.Application
import io.ktor.server.application.log
import java.net.URI
import java.util.UUID

data class CorsOrigin(
    val host: String,
    val scheme: String,
)

data class SecurityConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtRealm: String,
    val rateLimitLimit: Int,
    val rateLimitRefillSeconds: Long,
    val corsAllowedOrigins: List<CorsOrigin>,
    val adminHost: String,
    val adminPort: Int,
    val allowContainerAdminWildcardBind: Boolean,
    val adminCorsAllowedOrigins: List<CorsOrigin>,
    val adminTokenTtlSeconds: Long,
)

fun Application.loadSecurityConfig(appEnv: String): SecurityConfig =
    SecurityConfig(
        jwtSecret = resolveJwtSecret(appEnv),
        jwtIssuer = resolveConfig("JWT_ISSUER", "security.jwtIssuer", default = "http://localhost:18080"),
        jwtAudience = resolveConfig("JWT_AUDIENCE", "security.jwtAudience", default = "fictional-drug-ref"),
        jwtRealm = resolveConfig("JWT_REALM", "security.jwtRealm", default = "fictional-drug-ref"),
        rateLimitLimit = resolveConfig("RATE_LIMIT_LIMIT", "security.rateLimitLimit", default = "600").toInt(),
        rateLimitRefillSeconds = resolveConfig(
            "RATE_LIMIT_REFILL_SECONDS",
            "security.rateLimitRefillSeconds",
            default = "30",
        ).toLong(),
        corsAllowedOrigins = parseConfiguredCorsOrigins(
            raw = resolveConfig("CORS_ALLOWED_ORIGINS", "security.corsAllowedOrigins", default = ""),
            settingName = "security.corsAllowedOrigins",
        ),
        adminHost = validateAdminBindHost(
            adminHost = resolveConfig("ADMIN_HOST", "security.adminHost", default = "127.0.0.1"),
            allowContainerAdminWildcardBind = resolveConfig(
                "ALLOW_CONTAINER_ADMIN_WILDCARD_BIND",
                "security.allowContainerAdminWildcardBind",
                default = "false",
            ).toBooleanStrict(),
        ),
        adminPort = resolveConfig("ADMIN_PORT", "security.adminPort", default = "19090").toInt(),
        allowContainerAdminWildcardBind = resolveConfig(
            "ALLOW_CONTAINER_ADMIN_WILDCARD_BIND",
            "security.allowContainerAdminWildcardBind",
            default = "false",
        ).toBooleanStrict(),
        adminCorsAllowedOrigins = parseConfiguredCorsOrigins(
            raw = resolveConfig(
                "ADMIN_CORS_ALLOWED_ORIGINS",
                "security.adminCorsAllowedOrigins",
                default = "",
            ),
            settingName = "security.adminCorsAllowedOrigins",
        ),
        adminTokenTtlSeconds = resolveConfig(
            "ADMIN_TOKEN_TTL_SECONDS",
            "security.adminTokenTtlSeconds",
            default = "3600",
        ).toLong(),
    )

fun parseConfiguredCorsOrigins(
    raw: String,
    settingName: String,
): List<CorsOrigin> =
    raw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { parseCorsOrigin(it, settingName) }

fun parseCorsOrigin(
    origin: String,
    settingName: String,
): CorsOrigin {
    val uri = URI(origin)
    require(uri.scheme == "http" || uri.scheme == "https") {
        "$settingName entries must be absolute http or https origins: $origin"
    }
    require(!uri.host.isNullOrBlank()) {
        "$settingName entries must include a host: $origin"
    }
    require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
        "$settingName entries must be origins without user info, query, or fragment: $origin"
    }
    require(uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") {
        "$settingName entries must not include a path: $origin"
    }

    val host = if (uri.port == -1) uri.host else "${uri.host}:${uri.port}"
    return CorsOrigin(host = host, scheme = uri.scheme)
}

fun validateAdminBindHost(
    adminHost: String,
    allowContainerAdminWildcardBind: Boolean,
): String {
    val normalized = adminHost.trim()
    require(normalized.isNotEmpty()) { "security.adminHost must not be blank" }
    if (normalized == "0.0.0.0" || normalized == "::" || normalized == "[::]") {
        require(allowContainerAdminWildcardBind) {
            "security.adminHost=$normalized requires security.allowContainerAdminWildcardBind=true"
        }
        return normalized
    }
    require(normalized == "127.0.0.1" || normalized == "localhost" || normalized == "::1" || normalized == "[::1]") {
        "security.adminHost must be loopback-only unless it is an explicit container wildcard bind"
    }
    return normalized
}

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
