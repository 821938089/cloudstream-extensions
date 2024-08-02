package com.horis.cloudstreamplugins

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import okhttp3.Response
import okio.ByteString.Companion.decodeHex
import org.jsoup.nodes.Element

class CzzyProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    // www.czzy.site
    override var mainUrl = "https://www.czys.pro"
    override var name = "厂长资源"

    override val hasMainPage = true

    override val mainPage by lazy {
        mainPageOf(*mainPages.toList().map { (k, v) -> v to k }.toTypedArray())
    }

    private val mainPages = mapOf(
        "首页" to "$mainUrl/",
        "厂长推荐" to "$mainUrl/movie_bt%s",
        "近日更新" to "$mainUrl/movie_bt%s",
        "近日更新(电影)" to "$mainUrl/movie_bt_series/dyy%s",
        "国产剧集" to "$mainUrl/movie_bt_series/guochanju%s",
        "精品美剧" to "$mainUrl/movie_bt_series/mj%s",
        "精品韩剧" to "$mainUrl/movie_bt_series/hj%s",
        "精品日剧" to "$mainUrl/movie_bt_series/rj%s",
        "追番计划(动漫)" to "$mainUrl/movie_bt_view_cat/fjj%s",
        "PV预告" to "$mainUrl/movie_bt_view_cat/pvyugao%s",
    )

    private var cookies: Map<String, String> = mapOf()

    override var sequentialMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (page == 1 && request.name == "首页") {
            val res = app.get(mainUrl)
            val doc = res.document
            val swiperList = doc.select(".swiper-wrapper > div")
                .mapNotNull { it.toSearchResult() }
            val categories = doc.select(".mi_btcon")
            val homePages = arrayListOf<HomePageList>()
            homePages.add(HomePageList("首页", swiperList))
            categories.mapNotNullTo(homePages) { el ->
                val title = el.selectFirst("h2")?.text() ?: return@mapNotNullTo null
                val items = el.select("ul li").mapNotNull { it.toSearchResult() }
                HomePageList(title, items)
            }
            return HomePageResponse(homePages, true)
        } else if (page == 1 || request.name == "首页") {
            return null
        }

        val page1 = if (page - 1 == 1) "" else "/page/${page - 1}"
        val url = (mainPages[request.name] ?: return null).replace("%s", page1)
        val doc = app.get(url, referer = "$mainUrl/").document
        val items = doc.select(".mi_cont ul li").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        if (query.startsWith("http")) {
            return listOf(load(query)?.toSearchResponse() ?: return null)
        }
        return null
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val name = selectFirst("h3")?.text() ?: return null
        val url = selectFirst("a")?.attr("href") ?: return null
        return newTvSeriesSearchResponse(name, url) {
            val img = selectFirst("img")
            posterUrl = img?.attr("data-original")?.ifBlank {
                img.attr("src")
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, referer = "$mainUrl/", cookies = cookies).document
        val name = doc.selectFirst("h1")?.text() ?: error("解析数据失败（标题）")
        val desc = doc.select(".yp_context").text()

        val episodes = doc.select(".paly_list_btn a").mapNotNull {
            val url1 = fixUrl(it.attr("href"))
            newEpisode(url1) {
                this.name = it.text()
            }
        }
        return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
            posterUrl = doc.selectFirst(".dyimg img")?.attr("src")
            plot = desc
            tags = doc.selectFirst(".moviedteail_list li")?.select("a")?.map { it.text() }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val doc = app.get(data, referer = "$mainUrl/").document
        val url = doc.select("iframe").attr("href")
        val headers = mapOf(
            "Sec-Fetch-Dest" to "iframe",
            "Sec-Fetch-Mode" to "navigate"
        )
        val html = app.get(url, referer = "$mainUrl/", headers = headers).text
        val hex = html.substring("\"data\":\"", "\"").reversed()
        val playurlEncoded = hex.decodeHex().utf8()
        val slicePos = (playurlEncoded.length - 7) / 2
        val url1 = playurlEncoded.substring(0, slicePos)
        val url2 = playurlEncoded.substring(slicePos + 7)
        val playurl = url1 + url2

        callback(playurl.toLink())

        return true
    }

    private fun String.toLink(name: String = this@CzzyProvider.name): ExtractorLink {
        return ExtractorLink(
            this@CzzyProvider.name,
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
