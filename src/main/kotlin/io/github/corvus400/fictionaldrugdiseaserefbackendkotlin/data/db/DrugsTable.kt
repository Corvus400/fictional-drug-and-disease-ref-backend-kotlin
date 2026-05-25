package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.json.jsonb

object DrugsTable : Table("drugs") {
    val id = long("id").autoIncrement()
    val publicId = varchar("public_id", length = 64).uniqueIndex()
    val data = jsonb("data", AppJson, Drug.serializer())
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}
