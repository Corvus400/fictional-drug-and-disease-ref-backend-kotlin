package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DiseaseRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.ExposedDiseaseRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.ExposedDrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DiseaseListQueryService
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DrugListQueryService
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

fun Application.configureDI() {
    val appConfig = loadAppConfig()
    val databaseConfig = loadDatabaseConfig()
    dependencies {
        provide<AppConfig> { appConfig }
        provide<DatabaseConfig> { databaseConfig }
    }
}

fun Application.configureDataLayerDependencies(
    dataSource: DataSource,
    database: Database,
    databaseDispatcher: CoroutineDispatcher,
) {
    dependencies {
        provide<DataSource> { dataSource }
        provide<Database> { database }
        provide<CoroutineDispatcher> { databaseDispatcher }
        provide<DrugRepository> {
            ExposedDrugRepository(database = database, databaseDispatcher = databaseDispatcher)
        }
        provide<DiseaseRepository> {
            ExposedDiseaseRepository(database = database, databaseDispatcher = databaseDispatcher)
        }
        provide<DrugListQueryService> { DrugListQueryService(resolve()) }
        provide<DiseaseListQueryService> { DiseaseListQueryService(resolve()) }
    }
}
