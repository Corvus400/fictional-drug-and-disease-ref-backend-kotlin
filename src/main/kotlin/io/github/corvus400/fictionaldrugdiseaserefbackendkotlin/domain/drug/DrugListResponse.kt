package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.Disclaimer
import kotlinx.serialization.Serializable

/**
 * 医薬品一覧レスポンス — `/drugs` の返却型。サマリ配列 (`items`) とページング情報を保持する。
 *
 * 詳細画面遷移前の一覧表示に使用。完全な医薬品情報は `items[].id` で `/drugs/{id}` から取得。
 */
@Serializable
data class DrugListResponse(
    val items: List<DrugSummary>,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalCount: Int,
    val disclaimer: String = Disclaimer.SHORT,
)
