package com.horis.cloudstreamplugins.apiextractors

import com.fasterxml.jackson.annotation.JsonProperty
import com.horis.cloudstreamplugins.Category
import com.horis.cloudstreamplugins.Vod
import com.horis.cloudstreamplugins.apis.VodAPI
import com.lagradost.cloudstream3.ErrorLoadingException
import java.net.URLEncoder

class CollectAPIExtractor(private val api: VodAPI) : VodAPIExtractor {
    private var categoryCache: List<Category>? = null

    override suspend fun getCategory(limit: Int, skip: Int): List<Category> {
        categoryCache?.let { return it }
        val res = api.list()
        categoryCache = res.parsedSafe<CategoryList>()?.list?.drop(skip)?.take(limit)
            ?: throw ErrorLoadingException("获取分类数据失败 - 状态码${res.code}")
        return categoryCache!!
    }

    override suspend fun getVodList(
        query: String?,
        page: Int,
        ids: String?,
        type: String?,
        pageSize: Int?
    ): List<Vod>? {
        val encodedQuery = query?.let { URLEncoder.encode(it, "utf-8") }
        return api.list(encodedQuery, page, ids, type, pageSize).parsedSafe<VodList>()?.list
    }

    override suspend fun getVodListDetail(
        query: String?,
        page: Int,
        ids: String?,
        type: String?,
        pageSize: Int?
    ): List<Vod>? {
        val encodedQuery = query?.let { URLEncoder.encode(it, "utf-8") }
        return api.search(encodedQuery, page, ids, type, pageSize).parsedSafe<VodList>()?.list
    }

    data class VodList(
        @JsonProperty("list") val list: ArrayList<Vod>
    )

    data class CategoryList(
        @JsonProperty("class") val list: ArrayList<Category>
    )
}