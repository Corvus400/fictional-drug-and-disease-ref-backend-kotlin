package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.ktor.server.application.Application

data class ObservabilityConfig(
    val serviceName: String,
    val logLevel: String,
    val metricsAllowedCidrs: List<String>,
)

fun Application.loadObservabilityConfig(): ObservabilityConfig =
    ObservabilityConfig(
        serviceName = resolveConfig("OTEL_SERVICE_NAME", "observability.serviceName", default = "drug-disease-api"),
        logLevel = resolveConfig("LOG_LEVEL", "observability.logLevel", default = "INFO"),
        metricsAllowedCidrs = resolveConfig(
            "METRICS_ALLOWED_CIDRS",
            "observability.metricsAllowedCidrs",
            default = "127.0.0.1/32,::1/128,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16",
        ).split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() },
    )
