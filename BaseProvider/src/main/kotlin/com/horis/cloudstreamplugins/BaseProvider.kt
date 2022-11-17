package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.cloudstreamplugins.LoadRule
import com.lagradost.cloudstream3.cloudstreamplugins.SearchRule
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.nicehttp.NiceResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

abstract class BaseProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    open val mainPageRule: MainPageRule? = null
    open val searchRule: SearchRule? = null
    open val loadRule: LoadRule? = null

    open suspend fun fetchMainPage(page: Int, request: MainPageRequest): NiceResponse {
        throw NotImplementedError()
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        mainPageRule ?: return null
        val res = fetchMainPage(page, request)
        val list =
            mainPageRule!!.getList(getElements(res.text, mainPageRule!!.list), res) ?: return null
        val items = list.mapNotNull {
            it ?: return@mapNotNull null
            val url = fixUrl(
                mainPageRule!!.getUrl(getString(it, mainPageRule!!.url), res)
                    ?: return@mapNotNull null
            )
            val name = mainPageRule!!.getName(getString(it, mainPageRule!!.name), res)
                ?: return@mapNotNull null
            newTvSeriesSearchResponse(name, url) {
                posterUrl =
                    mainPageRule!!.getPosterUrl(getString(it, mainPageRule!!.posterUrl), res)
            }
        }

        return newHomePageResponse(request.name, items)
    }

    open suspend fun fetchSearch(query: String): NiceResponse {
        throw NotImplementedError()
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val finalSearchRule = searchRule ?: mainPageRule?.toSearchRule() ?: return null
        val res = fetchSearch(query)
        val list =
            finalSearchRule.getList(getElements(res.text, finalSearchRule.list), res) ?: return null
        val items = list.mapNotNull {
            it ?: return@mapNotNull null
            val url = fixUrl(
                finalSearchRule.getUrl(getString(it, finalSearchRule.url), res)
                    ?: return@mapNotNull null
            )
            val name = finalSearchRule.getName(getString(it, finalSearchRule.name), res)
                ?: return@mapNotNull null
            newTvSeriesSearchResponse(name, url) {
                posterUrl = fixUrlNull(
                    finalSearchRule.getPosterUrl(getString(it, finalSearchRule.posterUrl), res)
                )
            }
        }
        return items
    }

    open suspend fun fetchLoad(url: String): NiceResponse {
        throw NotImplementedError()
    }

    override suspend fun load(url: String): LoadResponse? {
        loadRule ?: return null
        val res = fetchLoad(url)

        val name = loadRule!!.getName(getString(res, loadRule!!.name), res) ?: return null
        val list = loadRule!!.getEpisodeList(getElements(res.text, loadRule!!.episodeList), res)
            ?: return null
        val items = list.mapNotNull {
            it ?: return@mapNotNull null
            val episodeUrl = fixUrl(
                loadRule!!.getEpisodeUrl(getString(it, loadRule!!.episodeUrl), res)
                    ?: return@mapNotNull null
            )
            val episodeName = loadRule!!.getEpisodeName(getString(it, loadRule!!.episodeName), res)
            newEpisode(episodeUrl) {
                this.name = episodeName
            }
        }
        return newTvSeriesLoadResponse(name, url, TvType.TvSeries, items) {
            year = loadRule!!.getYear(getString(res, loadRule!!.year), res)?.toIntOrNull()
            plot = loadRule!!.getPlot(getString(res, loadRule!!.plot), res)
            posterUrl =
                fixUrlNull(loadRule!!.getPosterUrl(getString(res, loadRule!!.posterUrl), res))
        }
    }

    private fun getElements(str: String, rule: String?): List<*>? {
        rule ?: return null
        if (rule.startsWith("@html:", true)) {
            val doc = Jsoup.parse(str)
            val rulePair = rule.substring(6).split("@", limit = 2)
            val selector = rulePair[0]
            return doc.select(selector)
        } else if (rule.startsWith("@json:", true)) {
            val keys = rule.substring(6).split(".")
            val data = tryParseJson<Map<*, *>>(str) as Any?
            val result = keys.fold(data) { obj, key ->
                when (obj) {
                    is Map<*, *> -> obj[key]
                    else -> obj
                }
            }
            return result as List<*>
        }
        return null
    }

    fun getString(data: Any?, rule: String?): String? {
        rule ?: return null
        if (rule.startsWith("@html:", true)) {
            val doc = if (data is NiceResponse) data.document else data as Element
            val (selector, attr) = rule.substring(6).split("@", limit = 2)
            return when (attr) {
                "text" -> doc.selectFirst(selector)?.text()?.trim()
                "data" -> doc.selectFirst(selector)?.data()?.trim()
                else -> doc.selectFirst(selector)?.attr(attr)?.trim()
            }
        } else if (rule.startsWith("@json:", true)) {
            val keys = rule.substring(6).split(".")
            return keys.fold(if (data is NiceResponse) tryParseJson<Map<*, *>>(data.text) else data) { obj, key ->
                when (obj) {
                    is Map<*, *> -> obj[key]
                    else -> obj
                }
            }?.toString()
        }
        return null
    }

}
