package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.cloudstreamplugins.SearchRule
import com.lagradost.nicehttp.NiceResponse

open class MainPageRule(
    val list: String? = null,
    val url: String? = null,
    val name: String? = null,
    val posterUrl: String? = null
) {
    open fun getList(list: List<*>?, res: NiceResponse) = list
    open fun getUrl(url: String?, res: NiceResponse) = url
    open fun getName(name: String?, res: NiceResponse) = name
    open fun getPosterUrl(posterUrl: String?, res: NiceResponse) = posterUrl

    fun toSearchRule() = SearchRule(
        list,
        url,
        name,
        posterUrl
    )
}


