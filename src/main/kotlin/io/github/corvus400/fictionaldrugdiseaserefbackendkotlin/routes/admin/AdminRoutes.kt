package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

@Serializable
data class WhoAmIResponse(
    val subject: String,
    val scopes: List<String>,
)

fun Route.adminRoutes() {
    get("/admin/whoami") {
        val principal = checkNotNull(call.principal<JWTPrincipal>())
        val scopes = principal.payload.getClaim("scope")
            .asString()
            ?.split(" ")
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        call.respond(
            WhoAmIResponse(
                subject = principal.payload.getClaim("sub").asString(),
                scopes = scopes,
            ),
        )
    }
}
