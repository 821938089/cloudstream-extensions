package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*

abstract class UAPIProvider : BaseUAPIProvider() {

    override suspend fun getCategory(): List<Category> {
        categoryCache?.let { return it }
        categoryCache =
            fetchApi("$mainUrl?ac=list").parsedSafe<CategoryList>()?.list?.take(8)
        return categoryCache ?: throw ErrorLoadingException("获取分类数据失败")
    }

    override suspend fun getVodList(url: String): List<Vod>? {
        return fetchApi(url).parsedSafe<VodList>()?.list
    }

    data class VodList(
        @JsonProperty("list") val list: ArrayList<Vod>
    )

    data class CategoryList(
        @JsonProperty("class") val list: ArrayList<Category>
    )

}
