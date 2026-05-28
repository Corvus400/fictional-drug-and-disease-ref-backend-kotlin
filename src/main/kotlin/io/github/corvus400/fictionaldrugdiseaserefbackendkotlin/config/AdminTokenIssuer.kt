package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Clock
import java.time.Instant
import java.util.Date

private const val ADMIN_SUBJECT = "local-admin"
private const val ADMIN_SCOPE = "admin"

data class MintedAdminToken(
    val accessToken: String,
    val expiresAt: Instant,
)

fun mintAdminToken(
    cfg: SecurityConfig,
    clock: Clock = Clock.systemUTC(),
): MintedAdminToken {
    val issuedAt = Instant.now(clock)
    val expiresAt = issuedAt.plusSeconds(cfg.adminTokenTtlSeconds)
    val token = JWT.create()
        .withIssuer(cfg.jwtIssuer)
        .withAudience(cfg.jwtAudience)
        .withSubject(ADMIN_SUBJECT)
        .withClaim("scope", ADMIN_SCOPE)
        .withIssuedAt(Date.from(issuedAt))
        .withExpiresAt(Date.from(expiresAt))
        .sign(Algorithm.HMAC256(cfg.jwtSecret))
    return MintedAdminToken(accessToken = token, expiresAt = expiresAt)
}
