package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DiseaseRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.CategoriesQueryService
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DiseaseListQueryService
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DrugListQueryService
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin.adminRoutes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.categories.categoriesRoutes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.common.dosageFormImageRoutes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.common.drugImageRoutes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.disease.diseaseRoutes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.drug.drugRoutes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import javax.sql.DataSource

private const val READINESS_DB_TIMEOUT_SECONDS = 1

fun Application.configureRouting() {
    val drugRepository: DrugRepository by dependencies
    val diseaseRepository: DiseaseRepository by dependencies
    val drugListService: DrugListQueryService by dependencies
    val diseaseListService: DiseaseListQueryService by dependencies
    val categoriesService: CategoriesQueryService by dependencies
    val observabilityConfig: ObservabilityConfig by dependencies
    val metricsRegistry: PrometheusMeterRegistry by dependencies
    val dataSource: DataSource by dependencies
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        get("/health/ready") {
            val ready = runCatching {
                dataSource.connection.use { connection ->
                    connection.isValid(READINESS_DB_TIMEOUT_SECONDS)
                }
            }.getOrDefault(false)
            if (ready) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "ready"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "not_ready"))
            }
        }
        route("/metrics") {
            install(MetricsAllowlist) {
                allowedCidrs = observabilityConfig.metricsAllowedCidrs
            }
            get {
                call.respondText(
                    text = metricsRegistry.scrape(),
                    contentType = ContentType.parse("text/plain; version=0.0.4"),
                )
            }
        }
        get("/robots.txt") {
            call.respondText(
                text = checkNotNull(this::class.java.classLoader.getResource("robots.txt")).readText(),
                contentType = ContentType.Text.Plain,
            )
        }
        route("/v1") {
            drugRoutes(repository = drugRepository, listService = drugListService)
            diseaseRoutes(repository = diseaseRepository, listService = diseaseListService)
            categoriesRoutes(service = categoriesService)
            dosageFormImageRoutes()
            drugImageRoutes()
        }
        authenticate(AUTH_JWT) {
            route("/v1") {
                requireScope("admin") {
                    adminRoutes()
                }
            }
        }
    }
}
