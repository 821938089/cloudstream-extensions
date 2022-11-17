package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.cloudstreamplugins.LoadRule
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.nicehttp.NiceResponse
import okhttp3.Interceptor
import okhttp3.Response

class LibvioProvider : BaseProvider() {
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

    override val mainPageRule = MainPageRule(
        list = "@html:ul.stui-vodlist > li",
        url = "@html:a@href",
        name = "@html:.title a@text",
        posterUrl = "@html:a@data-original"
    )

    //override val searchRule = mainPageRule.toSearchRule()

    override val loadRule = object : LoadRule(
        name = "@html:.title a@text",
        plot = "@html:.detail-content@text",
        posterUrl = "@html:.stui-content__thumb img@data-original",
        episodeList = "@html:.stui-vodlist__head h3, .stui-content__playlist a",
        episodeName = "@html:h3, a@text",
        episodeUrl = "@html:a@href"
    ) {

        override fun getYear(year: String?, res: NiceResponse): String? {
            getString(res, "@html:.stui-content__detail .data@text")?.let {
                return "年份：(\\d+)".toRegex().find(it)?.groupValues?.getOrNull(1)
            }
            return null
        }

        override fun getEpisodeName(episodeName: String?, res: NiceResponse): String? {
            episodeName ?: return null
            return if (episodeName == "猜你喜欢") null else episodeName
        }

        override fun getEpisodeUrl(episodeUrl: String?, res: NiceResponse): String? {
            return episodeUrl ?: ""
        }
    }

    override suspend fun fetchMainPage(page: Int, request: MainPageRequest): NiceResponse {
        val url = if (request.name == "首页") {
            request.data
        } else if (page < 1) {
            "${request.data}.html"
        } else {
            "${request.data}-$page.html"
        }
        return app.get(url, referer = "$mainUrl/")
    }


    override suspend fun fetchSearch(query: String): NiceResponse {
        val url = "$mainUrl/search/-------------.html?wd=$query&submit="
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