package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainException
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemDetails
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemTypes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<AdminGateNotFound> { call, _ ->
            call.respondCanonicalNotFound()
        }
        exception<DomainException> { call, cause ->
            call.respondProblem(cause.error.toProblem(call))
        }
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause
            call.application.log.error("Unhandled exception", cause)
            call.respondProblem(DomainError.Unexpected(cause).toProblem(call))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondCanonicalNotFound(status)
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            val retryAfter = call.response.headers["Retry-After"]
            call.respondProblem(
                ProblemDetails(
                    type = ProblemTypes.RATE_LIMITED,
                    title = "Too Many Requests",
                    status = status.value,
                    detail = if (retryAfter == null) {
                        "Rate limit exceeded."
                    } else {
                        "Rate limit exceeded. Retry after $retryAfter seconds."
                    },
                    instance = call.request.uri,
                ),
            )
        }
    }
}

suspend fun ApplicationCall.respondCanonicalNotFound(status: HttpStatusCode = HttpStatusCode.NotFound) {
    respondProblem(
        ProblemDetails(
            type = ProblemTypes.NOT_FOUND,
            title = "Resource not found",
            status = status.value,
            detail = "No route matched ${request.uri}",
            instance = request.uri,
        ),
    )
}

suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: AppResult<T>,
    successStatus: HttpStatusCode = HttpStatusCode.OK,
) = when (result) {
    is AppResult.Success -> respond(successStatus, result.value)
    is AppResult.Failure -> respondProblem(result.error.toProblem(this))
}

suspend fun ApplicationCall.respondProblem(problem: ProblemDetails) {
    respondText(
        text = AppJson.encodeToString(problem),
        contentType = ContentType("application", "problem+json"),
        status = HttpStatusCode.fromValue(problem.status),
    )
}

fun DomainError.toProblem(call: ApplicationCall): ProblemDetails = when (this) {
    is DomainError.NotFound -> ProblemDetails(
        type = ProblemTypes.NOT_FOUND,
        title = "Resource not found",
        status = HttpStatusCode.NotFound.value,
        detail = "$resource $id",
        instance = call.request.uri,
    )
    is DomainError.Validation -> ProblemDetails(
        type = ProblemTypes.VALIDATION,
        title = "Validation failed",
        status = HttpStatusCode.UnprocessableEntity.value,
        instance = call.request.uri,
        errors = violations,
    )
    is DomainError.UnsupportedMediaType -> ProblemDetails(
        type = ProblemTypes.UNSUPPORTED_MEDIA_TYPE,
        title = "Unsupported Media Type",
        status = HttpStatusCode.UnsupportedMediaType.value,
        detail = detail,
        instance = call.request.uri,
    )
    is DomainError.Conflict -> ProblemDetails(
        type = ProblemTypes.CONFLICT,
        title = "Conflict",
        status = HttpStatusCode.Conflict.value,
        detail = detail,
        instance = call.request.uri,
    )
    is DomainError.PreconditionFailed -> ProblemDetails(
        type = ProblemTypes.PRECONDITION_FAILED,
        title = "Precondition Failed",
        status = HttpStatusCode.PreconditionFailed.value,
        detail = detail,
        instance = call.request.uri,
    )
    DomainError.Unauthorized -> ProblemDetails(
        type = ProblemTypes.UNAUTHORIZED,
        title = "Unauthorized",
        status = HttpStatusCode.Unauthorized.value,
        instance = call.request.uri,
    )
    DomainError.Forbidden -> ProblemDetails(
        type = ProblemTypes.FORBIDDEN,
        title = "Forbidden",
        status = HttpStatusCode.Forbidden.value,
        instance = call.request.uri,
    )
    is DomainError.Unexpected -> ProblemDetails(
        type = ProblemTypes.INTERNAL,
        title = "Internal server error",
        status = HttpStatusCode.InternalServerError.value,
        instance = call.request.uri,
    )
}
