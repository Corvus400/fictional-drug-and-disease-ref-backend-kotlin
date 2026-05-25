package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.categories

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.ApiTag
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.respondResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.CategoriesResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.CategoriesQueryService
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route

fun Route.categoriesRoutes(service: CategoriesQueryService) {
    get("/categories", {
        summary = "全カテゴリメタデータを取得する"
        tags(ApiTag.CATEGORIES.tagName)
        description = "ATC / 薬効カテゴリ / 投与経路 / 剤形 / 規制区分 / ICD-10 章 / 診療科の 7 カテゴリを集約。"
        response {
            code(HttpStatusCode.OK) {
                description = "7 カテゴリのメタデータ"
                body<CategoriesResponse>()
            }
        }
    }) {
        call.respondResult(service.build())
    }
}
