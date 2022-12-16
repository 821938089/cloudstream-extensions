package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.nicehttp.NiceResponse
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

open class DesicinemasProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie
    )
    override var lang = "hi"

    override var mainUrl = "https://desicinemas.tv"
    override var name = "Desicinemas"

    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val doc = app.get(mainUrl).document

        val pages1 = doc.selectFirst(".MovieListTop")
            ?.toHomePageList("Most popular")

        val pages2 = doc.selectFirst("#home-movies-post")
            ?.toHomePageList("Latest Movies")

        return HomePageResponse(arrayListOf(pages1, pages2).filterNotNull())
    }

    private fun Element.toHomePageList(name: String): HomePageList {
        val items = select("li, .TPostMv")
            .mapNotNull {
                it.toHomePageResult()
            }
        return HomePageList(name, items)
    }

    private fun Element.toHomePageResult(): SearchResponse? {
        val title = selectFirst("h2")?.text()?.trim() ?: return null
        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val img = selectFirst("img")
        val posterUrl = fixUrlNull(img?.attr("data-src"))

        return newAnimeSearchResponse(title, href) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val doc = app.get(url, referer = "$mainUrl/").document

        val items = doc.select(".MovieList ul li").mapNotNull {
            it.toHomePageResult()
        }
        return items
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, referer = "$mainUrl/").document

        val title = doc.selectFirst("h1")?.text()?.trim() ?: return null
        val posterUrl = doc.select(".Image img").attr("src")

        val episodes = arrayListOf(newEpisode(url) {
            name = title
        })

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = fixUrlNull(posterUrl)
            plot = doc.selectFirst(".Description p")?.text()
            tags = doc.select(".Genre a").map { it.text() }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, referer = "$mainUrl/").document
        doc.select(".MovieList .OptionBx").amap {
            val link = it.select("a").attr("href")
            val name = it.select(".AAIco-dns, .AAIco-equalizer").text()
            var doc2 = app.get(link, referer = "$mainUrl/").document
            doc2.selectFirst("meta[HTTP-EQUIV=refresh]")?.let {
                val url = it.attr("content").substringAfter("URL=")
                doc2 = app.get(url, referer = data).document
            }
            val src = doc2.select("iframe").attr("src")
            loadExtractor(src, subtitleCallback, callback, name)
        }
        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor? {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request())
            }
        }
    }

}
