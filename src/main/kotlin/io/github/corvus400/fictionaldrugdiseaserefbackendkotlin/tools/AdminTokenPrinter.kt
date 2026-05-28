package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.tools

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.SecurityConfig
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.mintAdminToken

fun main() {
    val cfg = SecurityConfig(
        jwtSecret = System.getenv("JWT_SECRET") ?: error("JWT_SECRET is required to print an admin token"),
        jwtIssuer = System.getenv("JWT_ISSUER") ?: "http://localhost:18080",
        jwtAudience = System.getenv("JWT_AUDIENCE") ?: "fictional-drug-ref",
        jwtRealm = System.getenv("JWT_REALM") ?: "fictional-drug-ref",
        rateLimitLimit = 0,
        rateLimitRefillSeconds = 0,
        corsAllowedOrigins = emptyList(),
        adminPort = (System.getenv("ADMIN_PORT") ?: "19090").toInt(),
        adminCorsAllowedOrigins = emptyList(),
        adminTokenTtlSeconds = (System.getenv("ADMIN_TOKEN_TTL_SECONDS") ?: "3600").toLong(),
    )
    println(mintAdminToken(cfg).accessToken)
}
