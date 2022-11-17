package com.lagradost.cloudstream3.cloudstreamplugins

import com.lagradost.nicehttp.NiceResponse

open class LoadRule(
    val name: String? = null,
    val year: String? = null,
    val plot: String? = null,
    val posterUrl: String? = null,
    val episodeList: String? = null,
    val episodeName: String? = null,
    val episodeUrl: String? = null
) {
    open fun getName(name: String?, res: NiceResponse) = name
    open fun getYear(year: String?, res: NiceResponse) = year
    open fun getPlot(plot: String?, res: NiceResponse) = plot
    open fun getPosterUrl(posterUrl: String?, res: NiceResponse) = posterUrl
    open fun getEpisodeList(episodeList: List<*>?, res: NiceResponse) = episodeList
    open fun getEpisodeName(episodeName: String?, res: NiceResponse) = episodeName
    open fun getEpisodeUrl(episodeUrl: String?, res: NiceResponse) = episodeUrl
}
