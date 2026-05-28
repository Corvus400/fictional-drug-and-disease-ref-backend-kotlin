package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

private const val ID_DIGITS = 4

internal fun nextPublicId(
    existingIds: Iterable<String>,
    prefix: String,
): String {
    val nextNumber = existingIds
        .mapNotNull { id -> id.removePrefix(prefix).toIntOrNull() }
        .maxOrNull()
        ?.plus(1)
        ?: 0
    return prefix + nextNumber.toString().padStart(ID_DIGITS, '0')
}
