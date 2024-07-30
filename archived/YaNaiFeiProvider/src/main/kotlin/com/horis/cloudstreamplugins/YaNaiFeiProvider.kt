package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.nodes.Element

class YaNaiFeiProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    /**
     * 地址发布页
     * https://yayaym.com/
     * https://azx.me/
     */
    override var mainUrl = "https://yanetflix.tv"
    override var name = "鸭奈飞影视"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/vodshow/dianying--------" to "电影",
        "${mainUrl}/vodshow/lianxuju--------" to "电视剧",
        "${mainUrl}/vodshow/zongyi--------" to "综艺",
        "${mainUrl}/vodshow/dongman--------" to "动漫",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page == 1) {
            "${request.data}---.html"
        } else {
            "${request.data}$page---.html"
        }
        val document = app.get(url, referer = "$mainUrl/").document
        val items = document.select(".module > a").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst(".module-poster-item-title")?.text()?.trim() ?: return null
        val href = fixUrl(attr("href").toString())
        val posterUrl = fixUrlNull(selectFirst("img")?.attr("data-original"))

        return newAnimeSearchResponse(title, href) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/vodsearch/-------------.html?wd=$query"
        val document = app.get(url, referer = "$mainUrl/").document

        val items = document.select(".module-card-item").mapNotNull {
            val a = it.selectFirst(".module-card-item-title a") ?: return@mapNotNull null
            val name = a.text().trim()
            val href = a.attr("href")
            newMovieSearchResponse(name, href) {
                posterUrl = fixUrlNull(it.selectFirst("a img")?.attr("data-original"))
            }
        }
        return items
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, referer = "$mainUrl/").document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null

        val routeNames = document.select(".module-tab-item").mapIndexed { i, el ->
            val name = el.selectFirst("span")?.text()?.trim()
            val num = el.selectFirst("small")?.text()?.trim()
            SeasonData(i + 1, "$name - $num")
        }

        val episodes = document.select(".module-list").mapIndexedNotNull { i, el ->
            el.select("a").mapNotNull {
                val href = fixUrl(it.attr("href"))
                newEpisode(href) {
                    name = it.text().trim()
                    season = i + 1
                }
            }
        }.flatten()


        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            seasonNames = routeNames
            posterUrl =
                fixUrlNull(document.selectFirst(".module-item-pic img")?.attr("data-original"))
            tags = document.select(".module-info-tag a").map { it.text().trim() }
            plot = document.select(".module-info-introduction span").text().trim()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val html = app.get(data, referer = "$mainUrl/").text
        val vod = VodExtractor(html)
        val playUrl = vod.getPlayUrl()?.let {
            getPlayUrl(vod.playerInfo!!.from!!, it, data)
        } ?: return false
        callback(
            ExtractorLink(
                name,
                vod.playerInfo!!.from!!,
                playUrl,
                "",
                Qualities.Unknown.value,
                true
            )
        )
        return true
    }

    private suspend fun getPlayUrl(from: String, url: String, referer: String): String {
        val playUrl = when (from) {
            "NetflixOME", "QEys", "NetflixC", "dxzy", "netflixzx" -> "https://netflixku.4kya.com/?url=$url"
            "netflixmom", "netflixpro", "netflixmax" -> "https://player.4kya.com/?url=$url"
//            "NetflixD" -> "https://netflixku.4kya.com/player/?url=$url"
//            "netflixlv" -> "https://netflixvip.4kya.com/?url=$url"
            "rx" -> "https://rx.4kya.com/?url=$url"
            else -> return url
        }
        val html = app.get(playUrl, referer = referer).text
        val encryptedUrl = base64DecodeArray(html.substring("\"url\": \"", "\","))
        val tokenKey = when (from) {
            "QEys", "NetflixC", "dxzy", "netflixzx" -> "8FB5006902F91320"
            "NetflixOME", "netflixmom", "netflixpro", "netflixmax", "rx" -> "A42EAC0C2B408472"
            else -> throw ErrorLoadingException("未知视频播放来源")
        }.encodeToByteArray()
        val tokenIv = html.substring("le_token = \"", "\";").encodeToByteArray()
        return String(aesDecrypt(encryptedUrl, tokenKey, tokenIv))
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request().newBuilder().removeHeader("referer").build())
            }
        }
    }

}
