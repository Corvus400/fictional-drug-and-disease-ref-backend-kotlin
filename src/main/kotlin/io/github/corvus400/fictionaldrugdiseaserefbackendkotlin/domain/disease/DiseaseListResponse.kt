package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.Disclaimer
import kotlinx.serialization.Serializable

/**
 * 疾患一覧レスポンス: `/diseases` の返却型。サマリ配列 (`items`) とページング情報を保持する。
 *
 * 詳細画面遷移前の一覧表示に使用。完全な疾患情報は `items[].id` で `/diseases/{id}` から取得する。
 */
@Serializable
data class DiseaseListResponse(
    val items: List<DiseaseSummary>,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalCount: Int,
    val disclaimer: String = Disclaimer.SHORT,
)
