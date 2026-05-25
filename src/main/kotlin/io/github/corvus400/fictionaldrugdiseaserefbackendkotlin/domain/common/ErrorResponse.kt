package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: String? = null,
    val disclaimer: String = Disclaimer.SHORT,
)
