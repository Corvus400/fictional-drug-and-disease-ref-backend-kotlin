package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.disease

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.ApiTag
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.respondProblem
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.respondResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.toProblem
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DiseaseRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.DiseaseListResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Chronicity
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.ExamCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Icd10Chapter
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.MedicalDepartment
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.OnsetPattern
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DiseaseListQueryService
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.SearchDefaults
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route

fun Route.diseaseRoutes(
    repository: DiseaseRepository,
    listService: DiseaseListQueryService,
) {
    get("/diseases", {
        summary = "疾患一覧を取得する"
        tags(ApiTag.DISEASE.tagName)
        description = "実 DB の疾患一覧を envelope 形式で返す。page / page_size でページング可能。"
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
            queryParameter<String>("icd10_chapter") {
                description = "ICD-10 章コードの `@SerialName` 値 (例: `${Icd10Chapter.CHAPTER_I.serialName}`)。"
                required = false
            }
            queryParameter<String>("department") {
                description = "診療科の `@SerialName` 値 " +
                    "(例: `${MedicalDepartment.INTERNAL_MEDICINE.serialName}`)。"
                required = false
            }
            queryParameter<String>("chronicity") {
                description = "慢性度の `@SerialName` 値 (例: `${Chronicity.CHRONIC.serialName}`)。"
                required = false
            }
            queryParameter<String>("infectious") {
                description = "感染性の有無 (`true` / `false`)。不正値は 422。"
                required = false
            }
            queryParameter<String>("keyword") {
                description = "検索キーワード。空白区切りで複数語を渡すと AND 結合。"
                required = false
            }
            queryParameter<String>("keyword_match") {
                description = "一致モード `partial` (既定) / `prefix`。"
                required = false
            }
            queryParameter<String>("keyword_target") {
                description = "`name` (既定) / `name_english` / `synonyms` / `all`。"
                required = false
            }
            queryParameter<String>("sort") {
                description = "`-revised_at` (既定) / `name_kana` / `icd10_chapter`。未知値は 422。"
                required = false
            }
            queryParameter<String>("symptom_keyword") {
                description = "主症状キーワード (部分一致)。"
                required = false
            }
            queryParameter<String>("onset_pattern") {
                description = "発症パターンの enum 名 (例: `${OnsetPattern.ACUTE.name}`)。複数指定可。未知値は 422。"
                required = false
            }
            queryParameter<String>("exam_category") {
                description = "検査カテゴリの enum 名 (例: `${ExamCategory.IMAGING.name}`)。複数指定可。未知値は 422。"
                required = false
            }
            queryParameter<String>("has_pharmacological_treatment") {
                description = "薬物治療の有無 (`true` / `false`)。不正値は 422。"
                required = false
            }
            queryParameter<String>("has_severity_grading") {
                description = "重症度分類の有無 (`true` / `false`)。不正値は 422。"
                required = false
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "疾患一覧"
                body<DiseaseListResponse>()
            }
            code(HttpStatusCode.UnprocessableEntity) {
                description = "クエリパラメータ検証エラー"
            }
        }
    }) {
        when (val parsed = call.parseDiseaseListParams()) {
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

    get("/diseases/{id}", {
        summary = "疾患詳細を id で取得する"
        tags(ApiTag.DISEASE.tagName)
        description = "`id` で指定した疾患詳細を返す。"
        request {
            pathParameter<String>("id") {
                description = "疾患 ID (`disease_NNNN` 形式)"
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "`id` で指定された疾患"
                body<Disease>()
            }
            code(HttpStatusCode.NotFound) {
                description = "指定 id が存在しない"
            }
        }
    }) {
        call.respondResult(repository.findByPublicId(checkNotNull(call.parameters["id"])))
    }
}
