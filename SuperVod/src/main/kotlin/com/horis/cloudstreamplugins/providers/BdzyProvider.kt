package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SearchResponse

class BdzyProvider : UAPIProvider() {

    override var mainUrl = "https://api.apibdzy.com/api.php/provide/vod/"
    override var name = "百度影视"

    override suspend fun search(query: String): List<SearchResponse>? {
        throw ErrorLoadingException("不支持搜索")
    }

}
