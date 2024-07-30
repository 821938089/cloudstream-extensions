package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.nicehttp.NiceResponse
import kotlinx.coroutines.delay
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL

class SockshareProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.TvSeries,
        TvType.Movie
    )
    override var lang = "en"

    override var mainUrl = "https://sockshare.ac/"
    override var name = "Sockshare"

    override val hasMainPage = true

    private val categoryNames = arrayListOf(
        "MOVIES NOW PLAYING IN THEATERS",
        "RECENTLY ADDED MOVIES",
        "RECENTLY ADDED TV SERIES",
        "RECENTLY ADDED ANIME SERIES",
        "RECENTLY ADDED CARTOON MOVIES",
        "RECENTLY ADDED ASIAN DRAMAS"
    )

    private val cloudflareKiller by lazy { CloudflareKiller() }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val doc = fetchRetry(mainUrl).document

        val pages = categoryNames.mapNotNull {
            doc.selectFirst("div.content:has(span:contains($it))")?.toHomePageList(it)
        }

        return HomePageResponse(pages)
    }

    private fun Element.toHomePageList(name: String): HomePageList {
        val items = select(".listcontent li")
            .mapNotNull {
                it.toHomePageResult()
            }
        return HomePageList(name, items)
    }

    private fun Element.toHomePageResult(): SearchResponse? {
        val title = selectFirst("a.title")?.text()?.trim() ?: return null
        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val img = selectFirst("img")
        val posterUrl = fixUrlNull(img?.attr("src"))

        return newAnimeSearchResponse(title, href) {
            this.posterUrl = posterUrl
            posterHeaders = cloudflareKiller.getCookieHeaders(mainUrl).toMap()
        }
    }


    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search-movies/$query.html"
        val doc = fetchRetry(url, referer = "$mainUrl/").document

        val items = doc.select(".listcontent li").mapNotNull {
            it.toHomePageResult()
        }
        return items
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = fetchRetry(url, referer = "$mainUrl/").document

        val title = doc.selectFirst("#tit_player")?.text()?.trim() ?: return null
        val posterUrl = doc.select(".box_img img").attr("src")
        val episodesElement = doc.selectFirst(".section-box")

        val episodes = episodesElement?.select("a")?.map {
            newEpisode(it.attr("href")) {
                name = it.text()
            }
        } ?: arrayListOf(newEpisode(url) {
            name = title
        })

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = posterUrl
            posterHeaders = cloudflareKiller.getCookieHeaders(mainUrl).toMap()
            plot = doc.select(".box_des").text()
            tags = doc.select(".box_info span:containsOwn(Genres:) + span").text().split(",")
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = fetchRetry(data, referer = "$mainUrl/").document
        ioSafe {
            getIframeLink(doc)?.let {
                loadExtractor(it, subtitleCallback, callback)
            }
        }
        doc.select("#total_version a")
            .map { it.attr("href") }
            .filterIndexed { i, _ -> i > 0 }
            .amap {
                val link = getIframeLink(fetchRetry(it, referer = "$mainUrl/").document) ?: return@amap
                loadExtractor(link, subtitleCallback, callback)
            }
        return true
    }

    private fun getIframeLink(doc: Document): String? {
        return doc.selectFirst(".player script")?.data()
            ?.substring("Base64.decode(\"", "\"")
            ?.let { base64Decode(it).substring("src=\"", "\"") }
    }

    private suspend fun fetchRetry(url: String, referer: String? = null): NiceResponse {
        var retry = 2
        while (retry-- > 0) {
            try {
                return fetch(url, referer)
            } catch (e: Exception) {
                delay(1000)
            }
        }
        return fetch(url, referer)
    }

    private suspend fun fetch(url: String, referer: String? = null): NiceResponse {
        var res = app.get(url, referer = referer)
        if (res.document.select("title").text() == "Just a moment...") {
            res = app.get(url, referer = referer, interceptor = cloudflareKiller)
        }
        return res
    }

}
