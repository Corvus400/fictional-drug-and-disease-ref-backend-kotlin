package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.drug

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.ApiTag
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.etagFor
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.respondProblem
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.respondResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.toProblem
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.DrugListResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.PrecautionPopulationCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RegulatoryClass
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RouteOfAdministration
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.TherapeuticCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DrugListQueryService
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.SearchDefaults
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

fun Route.drugRoutes(
    repository: DrugRepository,
    listService: DrugListQueryService,
) {
    get("/drugs", {
        summary = "医薬品一覧を取得する"
        tags(ApiTag.DRUG.tagName)
        description = "実 DB の医薬品一覧を envelope 形式で返す。page / page_size でページング可能。"
        request {
            queryParameter<Int>("page") {
                description = "1-origin のページ番号 (既定 1)"
                required = false
            }
            queryParameter<Int>("page_size") {
                description = "1 ページの件数 (既定 ${SearchDefaults.DEFAULT_PAGE_SIZE}, " +
                    "上限 ${SearchDefaults.MAX_PAGE_SIZE})"
                required = false
            }
            queryParameter<String>("category_atc") {
                description = "ATC コードの先頭文字 (例: `A`)。指定時は前方一致で絞り込み"
                required = false
            }
            queryParameter<String>("regulatory_class") {
                description = "規制区分の `@SerialName` 値 " +
                    "(例: `${RegulatoryClass.PRESCRIPTION_REQUIRED.serialName}`)。"
                required = false
            }
            queryParameter<String>("route") {
                description = "投与経路の `@SerialName` 値 (例: `${RouteOfAdministration.ORAL.serialName}`)。"
                required = false
            }
            queryParameter<String>("dosage_form") {
                description = "剤形の `@SerialName` 値 (例: `${DosageForm.TABLET.serialName}`)。"
                required = false
            }
            queryParameter<String>("therapeutic_category") {
                description = "薬効カテゴリの enum 名 " +
                    "(例: `${TherapeuticCategory.ALIMENTARY_METABOLISM.name}`)。未知値は 422。"
                required = false
            }
            queryParameter<String>("keyword") {
                description = "検索キーワード。半角空白区切りの複数指定は AND 結合。"
                required = false
            }
            queryParameter<String>("keyword_match") {
                description = "キーワード一致モード。`partial` (既定) / `prefix`。"
                required = false
            }
            queryParameter<String>("keyword_target") {
                description = "`generic` / `brand` / `both` (既定) / `all`。"
                required = false
            }
            queryParameter<String>("sort") {
                description = "`-revised_at` (既定) / `brand_name_kana` / `atc_code` / " +
                    "`therapeutic_category_name`。"
                required = false
            }
            queryParameter<String>("adverse_reaction_keyword") {
                description = "副作用キーワード (部分一致)。"
                required = false
            }
            queryParameter<String>("precaution_category") {
                description = "特定背景患者カテゴリの enum 名 " +
                    "(例: `${PrecautionPopulationCategory.PREGNANT.name}`)。複数指定可。未知値は 422。"
                required = false
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "医薬品一覧"
                body<DrugListResponse>()
            }
            code(HttpStatusCode.UnprocessableEntity) {
                description = "クエリパラメータ検証エラー"
            }
        }
    }) {
        when (val parsed = call.parseDrugListParams()) {
            is AppResult.Failure -> call.respondProblem(parsed.error.toProblem(call))
            is AppResult.Success -> call.respondResult(
                listService.list(
                    query = parsed.value.query,
                    sort = parsed.value.sort,
                    page = parsed.value.page,
                    pageSize = parsed.value.pageSize,
                ),
            )
        }
    }

    get("/drugs/{id}", {
        summary = "医薬品詳細を id で取得する"
        tags(ApiTag.DRUG.tagName)
        description = "`id` で指定した医薬品詳細を返す。"
        request {
            pathParameter<String>("id") {
                description = "医薬品 ID (`drug_NNNN` 形式)"
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "`id` で指定された医薬品"
                body<Drug>()
            }
            code(HttpStatusCode.NotFound) {
                description = "指定 id が存在しない"
            }
        }
    }) {
        when (val result = repository.findWithMetaByPublicId(checkNotNull(call.parameters["id"]))) {
            is AppResult.Failure -> call.respondProblem(result.error.toProblem(call))
            is AppResult.Success -> {
                call.response.headers.append(HttpHeaders.ETag, etagFor(result.value.updatedAt))
                call.respond(HttpStatusCode.OK, result.value.entity)
            }
        }
    }
}
