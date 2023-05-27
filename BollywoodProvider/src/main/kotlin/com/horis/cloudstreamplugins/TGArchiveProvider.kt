package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import okhttp3.Response
import org.w3c.dom.Document

class TGArchiveProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "en"

    override var mainUrl = "https://tgarchive.eu.org"
    override var name = "TGArchive"

    override val hasMainPage = true

    private val headers = mapOf(
        "Referer" to "$mainUrl/",
    )

    private val api = "https://api.tgarchive.superfastsearch.zindex.eu.org"
    private val downloadApi = "https://hashhackers.dltelegram.workers.dev/"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        return newHomePageResponse("Home", getFiles(page).toSearchResponseList())
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        return app.get("$api/search?name=$query&page=1", headers = headers)
            .parsed<GDIndex>()
            .documents
            .toSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val file = tryParseJson<GDFile>(url) ?: return null
        val title = file.name

        val episodes = arrayListOf(newEpisode(file) {
            name = title
        })

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes)
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val file = parseJson<GDFile>(data)
        val token = getToken()
        val path = "$downloadApi/tgarchive/${file._id}?token=$token"
        callback(
            ExtractorLink(
                name,
                name,
                path,
                "$mainUrl/",
                Qualities.Unknown.value,
            )
        )
        return true
    }

    private suspend fun getToken(): String {
        val regex = """var newtime = "(.*?)"""".toRegex()
        val js = app.get(
            "https://geolocation.zindex.eu.org/api.js",
            referer = "$mainUrl/",
        ).text
        val match = regex.find(js) ?: throw ErrorLoadingException("parse api config fail")
        val newTime = match.groupValues[1]
        return newTime.reversed()
    }

    private fun List<GDFile>.toSearchResponseList(): List<SearchResponse> {
        return filter { it.name.contains("(?i)\\.(mkv|mp4)$".toRegex()) }.map {
            newAnimeSearchResponse(it.name, it.toJson())
        }
    }

//    @Suppress("ObjectLiteralToLambda")
//    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
//        return object : Interceptor {
//            override fun intercept(chain: Interceptor.Chain): Response {
//                val request = chain.request()
//                    .newBuilder()
//                    .removeHeader("referer")
//                    .build()
//                return chain.proceed(request)
//            }
//        }
//    }

    private fun error(msg: String = "加载数据失败"): Nothing {
        throw ErrorLoadingException(msg)
    }

    private suspend fun getFiles(page: Int = 1): List<GDFile> {
        return app.get("$api/index?page=$page", headers = headers)
            .parsed<GDIndex>()
            .documents
    }

    data class GDIndex(
        val documents: List<GDFile>,
        val page: Int
    )

    data class GDFile(
        val name: String,
        val _id: String,
    )

}
