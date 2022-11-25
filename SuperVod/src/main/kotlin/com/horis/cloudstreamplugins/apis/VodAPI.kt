package com.horis.cloudstreamplugins.apis

import com.lagradost.nicehttp.NiceResponse

interface VodAPI {

    suspend fun list(
        query: String? = null,
        page: Int = 0,
        ids: String? = null,
        type: String? = null,
        pageSize: Int? = null
    ): NiceResponse {
        throw NotImplementedError()
    }

    suspend fun search(
        query: String? = null,
        page: Int = 0,
        ids: String? = null,
        type: String? = null,
        pageSize: Int? = null
    ): NiceResponse {
        throw NotImplementedError()
    }

    suspend fun details(ids: String? = null, page: Int = 0, pageSize: Int? = null): NiceResponse {
        throw NotImplementedError()
    }

    suspend fun callApi(param: String): NiceResponse {
        throw NotImplementedError()
    }

    suspend fun playUrl(id: String): NiceResponse {
        throw NotImplementedError()
    }

}
