package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.PrecautionPopulationCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RegulatoryClass
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RouteOfAdministration
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.TherapeuticCategory

/**
 * `/drugs` 一覧エンドポイントの絞り込み条件。
 *
 * Phase 11-10a でフィルタが 8 個を超え detekt `LongParameterList` (max 8) に抵触したため、
 * `DrugListFixtures.resolve` のフィルタ系引数を本クラスに集約する。
 * 既存呼び出し側からは `DrugListQuery()` (全 null/既定) を渡せば従来同様のフィルタなし挙動。
 */
data class DrugListQuery(
    val atcPrefix: String? = null,
    val regulatoryClasses: List<RegulatoryClass> = emptyList(),
    val routes: List<RouteOfAdministration> = emptyList(),
    val dosageForms: List<DosageForm> = emptyList(),
    val therapeuticCategory: TherapeuticCategory? = null,
    val keyword: String? = null,
    val keywordMatch: KeywordMatch = KeywordMatch.PARTIAL,
    val keywordTarget: DrugKeywordTarget = DrugKeywordTarget.BOTH,
    val adverseReactionKeyword: String? = null,
    val precautionCategories: List<PrecautionPopulationCategory> = emptyList(),
)
