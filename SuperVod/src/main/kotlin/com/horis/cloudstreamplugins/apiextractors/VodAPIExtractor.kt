package com.horis.cloudstreamplugins.apiextractors

import com.horis.cloudstreamplugins.Category
import com.horis.cloudstreamplugins.Vod

interface VodAPIExtractor {

    suspend fun getCategory(limit: Int = 7, skip: Int = 0): List<Category> {
        throw NotImplementedError()
    }

    suspend fun getVodListDetail(
        query: String? = null,
        page: Int = 0,
        ids: String? = null,
        type: String? = null,
        pageSize: Int? = null
    ): List<Vod>? {
        throw NotImplementedError()
    }

    suspend fun getVodList(
        query: String? = null,
        page: Int = 0,
        ids: String? = null,
        type: String? = null,
        pageSize: Int? = null
    ): List<Vod>? {
        throw NotImplementedError()
    }

}