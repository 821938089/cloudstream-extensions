package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.nodes.Element

class LibvioProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    override var mainUrl = "https://www.libvio.me"
    override var name = "LIBVIO"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        mainUrl to "首页",
        "${mainUrl}/type/1" to "电影",
        "${mainUrl}/type/2" to "剧集",
        "${mainUrl}/type/4" to "动漫",
        "${mainUrl}/type/15" to "日韩剧",
        "${mainUrl}/type/16" to "欧美剧",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (request.name == "首页") {
            request.data
        } else if (page < 1) {
            "${request.data}.html"
        } else {
            "${request.data}-$page.html"
        }
        val doc = app.get(url, referer = "$mainUrl/").document
        val items = doc.select("ul.stui-vodlist > li").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val url = "$mainUrl/search/-------------.html?wd=$query&submit="
        val doc = app.get(url, referer = "$mainUrl/").document
        return doc.select("ul.stui-vodlist > li").mapNotNull {
            it.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val name = selectFirst(".title a")?.text() ?: return null
        val a = selectFirst("a")
        val url = a?.attr("href") ?: return null
        return newTvSeriesSearchResponse(name, url) {
            posterUrl = a.attr("data-original")
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, referer = "$mainUrl/").document
        val name = doc.selectFirst(".title a")?.text() ?: return null
        val episodes = ArrayList<Episode>()
        for (it in doc.select(".stui-vodlist__head")) {
            val route = it.selectFirst("h3")?.text() ?: continue
            if (route == "猜你喜欢") continue
            for (ep in it.select(".stui-content__playlist a")) {
                val episodeUrl = ep.selectFirst("a")?.attr("href") ?: continue
                episodes.add(newEpisode(episodeUrl) {
                    this.name = ep.selectFirst("a")!!.text()
                })
            }
        }
        return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
            doc.selectFirst(".stui-content__detail .data")?.text()?.let {
                year = "年份：(\\d+)".toRegex().find(it)?.groupValues?.getOrNull(1)?.toIntOrNull()
            }
            plot = doc.selectFirst(".detail-content")?.text()
            posterUrl = doc.selectFirst(".stui-content__thumb img")?.attr("data-original")
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
            AppUtils.tryParseJson<PlayData>(script)?.let { playData ->
                playData.url ?: return@let
                playData.link ?: return@let
                val js = app.get("$mainUrl/static/player/${playData.from}.js?v=1.3").text
                val src = js.substring("src=\"", "'")
                val html = app.get(
                    "$src${playData.url}&next=${playData.link_next}&id=${playData.id}&nid=${playData.nid}",
                    referer = "$mainUrl/"
                ).text
                val m3u8Url = html.substring("var urls = '", "';")
                M3u8Helper.generateM3u8(name, m3u8Url, "")
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

    data class PlayData(
        @JsonProperty("url") val url: String?,
        @JsonProperty("link") val link: String?,
        @JsonProperty("link_next") val link_next: String?,
        @JsonProperty("id") val id: String?,
        @JsonProperty("nid") val nid: String?,
        @JsonProperty("from") val from: String?
    )
}

fun String.substring(left: String, right: String): String {
    return substringAfter(left).substringBefore(right)
}