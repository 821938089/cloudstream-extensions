package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import okhttp3.Response
import okio.ByteString.Companion.decodeHex
import org.jsoup.nodes.Element

class ZxzjProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    // www.zxzj.site
    override var mainUrl = "https://www.zxzj.pro"
    override var name = "在线之家"

    override val hasMainPage = true

    override val mainPage by lazy {
        mainPageOf(*mainPages.toList().map { (k, v) -> v to k }.toTypedArray())
    }

    private val mainPages = mapOf(
        "首页" to "$mainUrl/",
        "电影" to "$mainUrl/list/1%s.html",
        "美剧" to "$mainUrl/list/2%s.html",
        "韩剧" to "$mainUrl/list/3%s.html",
        "日剧" to "$mainUrl/list/4%s.html",
        "泰剧" to "$mainUrl/list/5%s.html",
        "动漫" to "$mainUrl/list/6%s.html",
    )

    private var cookies: Map<String, String> = mapOf()

    override var sequentialMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (page == 1 && request.name == "首页") {
            val res = app.get(mainUrl)
            val doc = res.document
            val elements = doc.select(".stui-pannel__bd > *")
            val first = elements.removeAt(0)
            elements.removeAt(elements.lastIndex)
            val categories = elements.chunked(2)
            val homePages = arrayListOf<HomePageList>()
            val firstItems = first.select("ul li").mapNotNull { it.toSearchResult() }
            homePages.add(HomePageList("首页", firstItems))
            categories.mapNotNullTo(homePages) { (header, container) ->
                val title = header.selectFirst("h3")?.text() ?: return@mapNotNullTo null
                val items = container.select("ul li").mapNotNull { it.toSearchResult() }
                HomePageList(title, items)
            }
            return HomePageResponse(homePages, true)
        } else if (page == 1 || request.name == "首页") {
            return null
        }

        val page1 = if (page - 1 == 1) "" else "-${page - 1}"
        val url = (mainPages[request.name] ?: return null).replace("%s", page1)
        val doc = app.get(url, referer = "$mainUrl/").document
        val items = doc.select(".stui-vodlist li").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val url = "$mainUrl/vodsearch/-------------.html?wd=$query&submit="
        val doc = app.get(url, referer = "$mainUrl/").document
        return doc.select("ul.stui-vodlist > li").mapNotNull {
            it.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val name = selectFirst("h4")?.text() ?: return null
        val url = selectFirst("a")?.attr("href") ?: return null
        return newTvSeriesSearchResponse(name, url) {
            val img = selectFirst("a")
            posterUrl = img?.attr("data-original")?.ifBlank {
                img.attr("src")
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, referer = "$mainUrl/", cookies = cookies).document
        val name = doc.selectFirst("h1")?.text() ?: error("解析数据失败（标题）")
        val routes = doc.select(".stui-vodlist__head, .stui-content__playlist")
            .chunked(2)
            .filter { it.size == 2 }
        val routeNames = arrayListOf<SeasonData>()
        val episodes = routes.mapIndexedNotNull { i, (head, playlist) ->
            val routeName = head.select("h3").text()
            if (routeName.contains("网盘")) return@mapIndexedNotNull null
            routeNames.add(SeasonData(i + 1, routeName))
            playlist.select("a").map {
                val url1 = fixUrl(it.attr("href"))
                newEpisode(url1) {
                    this.name = it.text()
                    season = i + 1
                }
            }
        }.flatten()
        return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
            posterUrl = doc.selectFirst(".pic img")?.attr("data-original")
            seasonNames = routeNames
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        var html = app.get(data, referer = "$mainUrl/").text
        val vod = VodExtractor(html)
        val playData = vod.playerInfo ?: return false
        val url = playData.url ?: return false
        val headers = mapOf(
            "Sec-Fetch-Dest" to "iframe",
            "Sec-Fetch-Mode" to "navigate"
        )
        html = app.get(url, referer = "$mainUrl/", headers = headers).text
        val hex = html.substring("\"data\":\"", "\"").reversed()
        val playurlEncoded = hex.decodeHex().utf8()
        val slicePos = (playurlEncoded.length - 7) / 2
        val url1 = playurlEncoded.substring(0, slicePos)
        val url2 = playurlEncoded.substring(slicePos + 7)
        val playurl = url1 + url2

        callback(playurl.toLink())

        return true
    }

    private fun String.toLink(name: String = this@ZxzjProvider.name): ExtractorLink {
        return ExtractorLink(
            this@ZxzjProvider.name,
            name,
            this,
            "",
            Qualities.Unknown.value,
            this.contains(".m3u8"),
        )
    }

    private fun LoadResponse.toSearchResponse(): SearchResponse {
        return newTvSeriesSearchResponse(name, url) {
            posterUrl = this@toSearchResponse.posterUrl
        }
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request().newBuilder().removeHeader("referer").build())
            }
        }
    }

    private fun error(msg: String = "加载数据失败"): Nothing {
        throw ErrorLoadingException(msg)
    }

}
