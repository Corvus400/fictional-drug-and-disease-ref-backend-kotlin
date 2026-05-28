package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.TextColumnType

internal fun Expression<*>.jsonbArrayContains(
    fieldName: String,
    value: String,
): Op<Boolean> = JsonbArrayContainsOp(column = this, fieldName = fieldName, value = value)

private class JsonbArrayContainsOp(
    private val column: Expression<*>,
    private val fieldName: String,
    private val value: String,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append("(")
        append(column)
        append(" -> ")
        registerArgument(TextColumnType(), fieldName)
        append(") @> ")
        registerArgument(TextColumnType(), AppJson.encodeToString(listOf(value)))
        append("::jsonb")
    }
}
