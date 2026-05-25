package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
/**
 * RFC 9457 problem+json。
 * AppJson は encodeDefaults=true のため、optional は @EncodeDefault(NEVER) で null 時に JSON から省く。
 */
data class ProblemDetails(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val type: String = ProblemTypes.ABOUT_BLANK,
    val title: String,
    val status: Int,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val detail: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val instance: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val errors: List<FieldViolation>? = null,
)

/** type URI の SSOT。category は DomainError 6 種に対応。 */
object ProblemTypes {
    const val ABOUT_BLANK: String = "about:blank"
    private const val BASE: String =
        "https://github.com/Corvus400/fictional-drug-and-disease-ref/problems"
    const val NOT_FOUND: String = "$BASE/not-found"
    const val VALIDATION: String = "$BASE/validation"
    const val CONFLICT: String = "$BASE/conflict"
    const val UNAUTHORIZED: String = "$BASE/unauthorized"
    const val FORBIDDEN: String = "$BASE/forbidden"
    const val INTERNAL: String = "$BASE/internal"
}
