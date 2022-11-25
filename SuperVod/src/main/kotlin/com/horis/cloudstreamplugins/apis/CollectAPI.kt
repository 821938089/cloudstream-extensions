package com.horis.cloudstreamplugins.apis

import com.horis.cloudstreamplugins.app
import com.lagradost.nicehttp.NiceResponse

class CollectAPI(private val apiUrl: String) : VodAPI {

    companion object {
        const val UserAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36"
    }

    override suspend fun list(
        query: String?,
        page: Int,
        ids: String?,
        type: String?,
        pageSize: Int?
    ): NiceResponse {
        return callApi(query, page, ids, type, pageSize, "list")
    }

    override suspend fun search(
        query: String?,
        page: Int,
        ids: String?,
        type: String?,
        pageSize: Int?
    ): NiceResponse {
        return callApi(query, page, ids, type, pageSize)
    }

    override suspend fun details(ids: String?, page: Int, pageSize: Int?): NiceResponse {
        return callApi(ids = ids, page = page, pageSize = pageSize)
    }

    private suspend fun callApi(
        query: String? = null,
        page: Int = 0,
        ids: String? = null,
        type: String? = null,
        pageSize: Int? = null,
        action: String = "videolist"
    ): NiceResponse {
        var param = "?ac=$action"
        if (query != null) {
            param += "&wd=$query"
        } else if (ids != null) {
            param += "&ids=$ids"
        }
        if (page > 0) {
            param += "&pg=$page"
        }
        type?.let {
            param += "&t=$type"
        }
        pageSize?.let {
            param += "&pagesize=$pageSize"
        }
        return fetchApi("$apiUrl$param")
    }

    private suspend fun fetchApi(
        url: String,
        params: Map<String, String> = mapOf(),
        headers: Map<String, String> = mapOf("User-Agent" to UserAgent),
        retry: Int = 3
    ): NiceResponse {
        var retry1 = retry - 1
        while (retry1-- > 0) {
            try {
                return app.get(url, headers, referer = url, params = params, verify = false)
            } catch (_: Exception) {
            }
        }
        return app.get(url, headers, referer = url, params = params, verify = false)
    }

}
