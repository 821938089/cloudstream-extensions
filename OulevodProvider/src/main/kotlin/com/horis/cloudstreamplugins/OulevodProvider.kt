package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.cloudstreamplugins.LoadRule
import com.lagradost.cloudstream3.cloudstreamplugins.SearchRule
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.nicehttp.NiceResponse
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

class OulevodProvider : BaseProvider() {

    override var mainUrl = "https://www.oulevod.tv"
    override var name = "欧乐影院"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/index.php/vod/show/id/1" to "电影",
        "${mainUrl}/index.php/vod/show/id/2" to "电视剧",
        "${mainUrl}/index.php/vod/show/id/3" to "动漫",
        "${mainUrl}/index.php/vod/show/id/4" to "综艺",
    )

    override val mainPageRule = MainPageRule(
        list = "@html:ul.hl-vod-list.clearfix > li",
        url = "@html:a@href",
        name = "@html:.hl-item-text a@text",
        posterUrl = "@html:a@data-original"
    )

    override val searchRule = SearchRule(
        list = "@html:ul.hl-one-list li",
        url = "@html:.hl-item-title a@text",
        name = "@html:.hl-item-title a@href",
        posterUrl = "@html:a.hl-item-thumb@data-original"
    )

    override val loadRule = object : LoadRule(
        name = "@html:.hl-dc-title@text",
        posterUrl = "@html:.hl-dc-pic span@data-original",
        episodeList = "@html:.hl-tabs-box li a",
        episodeName = "@html:a@text",
        episodeUrl = "@html:a@href"
    ) {
        override fun getYear(year: String?, res: NiceResponse): String? {
            return (res.document.select(".hl-full-box ul li").getOrNull(4)
                ?.childNode(1) as TextNode).wholeText
        }

        override fun getPlot(plot: String?, res: NiceResponse): String? {
            return (res.document.select(".hl-full-box ul li").lastOrNull()
                ?.childNode(1) as TextNode).wholeText
        }
    }

    override suspend fun fetchMainPage(page: Int, request: MainPageRequest): NiceResponse {
        val url = if (page < 1) {
            "${request.data}.html"
        } else {
            "${request.data}/page/$page.html"
        }
        return app.get(url, referer = "$mainUrl/")
    }

    override suspend fun fetchSearch(query: String): NiceResponse {
        val url = "$mainUrl/index.php/vod/search.html?wd=$query&submit="
        return app.get(url, referer = "$mainUrl/")
    }

    override suspend fun fetchLoad(url: String): NiceResponse {
        return app.get(url, referer = "$mainUrl/")
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