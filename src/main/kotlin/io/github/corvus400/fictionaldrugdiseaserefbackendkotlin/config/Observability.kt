package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import inet.ipaddr.AddressStringException
import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressString
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainException
import io.ktor.server.application.Application
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.request.ApplicationRequest
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.configureObservability(registry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        this.registry = registry
    }
}

class MetricsAllowlistConfig {
    var allowedCidrs: List<String> = emptyList()
}

val MetricsAllowlist = createRouteScopedPlugin("MetricsAllowlist", ::MetricsAllowlistConfig) {
    val allowedRanges = pluginConfig.allowedCidrs.mapNotNull { cidr ->
        parseIpAddress(cidr)
            .also { parsed ->
                if (parsed == null) {
                    application.log.warn("Invalid metrics allowlist CIDR ignored: {}", cidr)
                }
            }
    }

    onCall { call ->
        val peerHost = call.request.realPeerHost()
        if (!isMetricsPeerAllowedRanges(peerHost, allowedRanges)) {
            call.application.log.warn("Rejected /metrics request from non-allowlisted peer: {}", peerHost)
            throw DomainException(DomainError.Forbidden)
        }
    }
}

fun isMetricsPeerAllowed(
    peerHost: String,
    allowedCidrs: List<String>,
): Boolean = isMetricsPeerAllowedRanges(peerHost, allowedCidrs.mapNotNull(::parseIpAddress))

private fun isMetricsPeerAllowedRanges(
    peerHost: String,
    allowedRanges: List<IPAddress>,
): Boolean {
    val peerAddress = parseIpAddress(peerHost) ?: return false
    return allowedRanges.any { it.prefixContains(peerAddress) }
}

private fun parseIpAddress(raw: String): IPAddress? =
    try {
        val normalized = when (raw) {
            "localhost" -> "127.0.0.1"
            else -> raw
        }
        IPAddressString(normalized).toAddress()
    } catch (_: AddressStringException) {
        null
    }

private fun ApplicationRequest.realPeerHost(): String = local.remoteAddress
