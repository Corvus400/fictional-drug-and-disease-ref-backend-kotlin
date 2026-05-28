package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Options
import io.ktor.server.application.Application
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.request.path
import io.ktor.server.routing.Route
import kotlin.time.Duration.Companion.seconds

const val AUTH_JWT: String = "auth-jwt"
val PUBLIC_RATE_LIMIT = RateLimitName("public")

fun Application.configureForwardedHeaders() {
    install(XForwardedHeaders)
}

fun Application.configureSecurity() {
    val cfg: SecurityConfig by dependencies
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = cfg.jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(cfg.jwtSecret))
                    .withIssuer(cfg.jwtIssuer)
                    .withAudience(cfg.jwtAudience)
                    .build(),
            )
            validate { credential ->
                if (credential.payload.getClaim("sub").asString().isNullOrBlank()) {
                    null
                } else {
                    JWTPrincipal(credential.payload)
                }
            }
            challenge { _, _ ->
                call.respondProblem(DomainError.Unauthorized.toProblem(call))
            }
        }
    }
}

fun Application.configureRateLimit() {
    val cfg: SecurityConfig by dependencies
    install(RateLimit) {
        global {
            rateLimiter(limit = cfg.rateLimitLimit, refillPeriod = cfg.rateLimitRefillSeconds.seconds)
            requestKey { call ->
                call.request.headers["CF-Connecting-IP"]
                    ?: call.request.origin.remoteHost
            }
            requestWeight { call, _ ->
                if (call.request.path().startsWith("/v1/") && !call.request.path().startsWith("/v1/admin/")) {
                    1
                } else {
                    0
                }
            }
        }
    }
}

fun Route.installCors(
    allowedOrigins: List<CorsOrigin>,
    allowedMethods: List<HttpMethod>,
    exposedHeaders: List<String> = emptyList(),
) {
    if (allowedOrigins.isEmpty()) return
    install(CORS) {
        allowedOrigins.forEach { origin ->
            allowHost(origin.host, schemes = listOf(origin.scheme))
        }
        (allowedMethods + Options).distinct().forEach(::allowMethod)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        exposedHeaders.forEach(::exposeHeader)
    }
}

class AdminPortGateConfig {
    var adminPort: Int = 0
}

class AdminGateNotFound : RuntimeException()

val AdminPortGate = createRouteScopedPlugin("AdminPortGate", ::AdminPortGateConfig) {
    onCall { call ->
        if (call.request.local.localPort != pluginConfig.adminPort) {
            throw AdminGateNotFound()
        }
    }
}

class ScopeConfig {
    var required: String = ""
}

val ScopeAuthorization = createRouteScopedPlugin("ScopeAuthorization", ::ScopeConfig) {
    on(AuthenticationChecked) { call ->
        val scopes = call.principal<JWTPrincipal>()
            ?.payload
            ?.getClaim("scope")
            ?.asString()
            ?.split(" ")
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        if (pluginConfig.required !in scopes) {
            throw DomainException(DomainError.Forbidden)
        }
    }
}

fun Route.requireScope(
    scope: String,
    build: Route.() -> Unit,
) {
    install(ScopeAuthorization) {
        required = scope
    }
    build()
}
