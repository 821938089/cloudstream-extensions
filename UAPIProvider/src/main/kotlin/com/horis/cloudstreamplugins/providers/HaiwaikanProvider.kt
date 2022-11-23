package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SearchResponse
import java.net.URLEncoder


class HaiwaikanProvider : UAPIProvider() {

    override var mainUrl = "http://api.haiwaikan.com/v1/vod"
    override var name = "海外看影视"
    override val playFromFilter = hashSetOf("haiwaikan")

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun search(query: String): List<SearchResponse>? {
        val encodeQuery = URLEncoder.encode(query, "utf-8")
        val vodList = getVodList("$mainUrl?ac=list&wd=$encodeQuery")
                ?: throw ErrorLoadingException("获取搜索数据失败")
        val ids = vodList.map { it.id }.joinToString(",")
        return super.search(ids)
    }

}
