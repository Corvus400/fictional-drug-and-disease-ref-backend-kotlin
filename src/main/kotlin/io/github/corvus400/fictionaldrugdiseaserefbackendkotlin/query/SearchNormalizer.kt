package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import java.text.Normalizer

fun searchNormalize(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC)
        .map { char -> char.toKatakanaIfHiragana() }
        .joinToString(separator = "")

private fun Char.toKatakanaIfHiragana(): Char =
    if (this in HIRAGANA_START..HIRAGANA_END) {
        (code + HIRAGANA_TO_KATAKANA_OFFSET).toChar()
    } else {
        this
    }

private const val HIRAGANA_START: Char = 'ぁ'
private const val HIRAGANA_END: Char = 'ゖ'
private const val HIRAGANA_TO_KATAKANA_OFFSET: Int = 0x60
