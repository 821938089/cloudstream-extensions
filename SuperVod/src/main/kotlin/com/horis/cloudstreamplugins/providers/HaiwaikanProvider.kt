package com.horis.cloudstreamplugins.providers

import com.horis.cloudstreamplugins.BaseVodProvider
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SearchResponse


class HaiwaikanProvider : BaseVodProvider() {

    override var mainUrl = "http://api.haiwaikan.com/v1/vod"
    override var name = "海外看影视"
    override val playFromFilter = hashSetOf("haiwaikan")

    override suspend fun search(query: String): List<SearchResponse>? {
        val vodList = apiExtractor.getVodList(query = query)
                ?: throw ErrorLoadingException("获取搜索数据失败")
        val ids = vodList.map { it.id }.joinToString(",")
        return super.search(ids)
    }

}
