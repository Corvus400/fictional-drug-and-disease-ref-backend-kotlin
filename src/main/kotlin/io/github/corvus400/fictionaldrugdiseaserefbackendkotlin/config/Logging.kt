package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.UUID
import ch.qos.logback.classic.Level as LogbackLevel
import ch.qos.logback.classic.Logger as LogbackLogger

fun Application.configureLogging() {
    val logLevel = resolveConfig("LOG_LEVEL", "observability.logLevel", default = "INFO").uppercase()
    val callLogLevel = Level.valueOf(logLevel)
    val rootLogger = LoggerFactory.getLogger(LogbackLogger.ROOT_LOGGER_NAME) as? LogbackLogger
    rootLogger?.level = LogbackLevel.valueOf(logLevel)
    install(CallId) {
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        level = callLogLevel
        callIdMdc("call_id")
    }
}
