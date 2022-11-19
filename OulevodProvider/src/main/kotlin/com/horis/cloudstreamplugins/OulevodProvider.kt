package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.nodes.Element

class OulevodProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    override var mainUrl = "https://www.oulevod.tv"
    override var name = "欧乐影院"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/index.php/vod/show/id/1" to "电影",
        "${mainUrl}/index.php/vod/show/id/2" to "电视剧",
        "${mainUrl}/index.php/vod/show/id/3" to "动漫",
        "${mainUrl}/index.php/vod/show/id/4" to "综艺",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = request.data
        val document = app.get(url).document
        val items = document.select("ul.hl-vod-list.clearfix > li").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst(".hl-item-text")?.text()?.trim() ?: return null
        val href = fixUrl(selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(selectFirst("a")?.attr("data-original"))

        return newAnimeSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/index.php/vod/search.html?wd=$query&submit=").document
        val items = document.select("ul.hl-one-list").mapNotNull {
            val name = it.selectFirst(".hl-item-content a")?.text()?.trim()
                ?: return@mapNotNull null
            val url = it.selectFirst("a")?.attr("href")
                ?: return@mapNotNull null
            newMovieSearchResponse(name, url)
        }
        return items
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst(".hl-dc-title")?.text()?.trim() ?: return null
//        val tvType = if (document.selectFirst(".hl-text-conch.active")?.text() == "电影") {
//            TvType.Movie
//        } else {
//            TvType.TvSeries
//        }
        val episodes = document.select(".hl-tabs-box li a").map {
            val name = it.text()
            val href = it.attr("href")
            Episode(name = name, data = href)
        }
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            posterUrl = fixUrlNull(document.selectFirst(".hl-dc-pic span")?.attr("data-original"))
            year = document.select(".hl-full-box ul li").getOrNull(4)?.text()?.toIntOrNull()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        var script = document.select("script").firstOrNull {
            it.data().indexOf("var player_aaaa=") > -1
        }?.data()
        if (script != null) {
            script = script.replace("var player_aaaa=", "")
            AppUtils.tryParseJson<Source>(script)?.let { source ->
                source.url ?: return@let
                M3u8Helper.generateM3u8(name, source.url, "")
                    .forEach(callback)
            }
        }
        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request().newBuilder().removeHeader("referer").build())
            }
        }
    }

    data class Source(
        @JsonProperty("url") val url: String?
    )
}