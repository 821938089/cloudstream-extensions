package com.horis.cloudstreamplugins

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class PaheProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.AnimeMovie,
        TvType.OVA,
        TvType.Anime,
    )
    override var lang = "en"

    override var mainUrl = "https://pahe.sbs"
    override var name = "Pahe"

    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val doc = app.get(mainUrl).document

        val recommendPages = doc.selectFirst(".gmr-owl-wrap")?.toHomePageList("Recommend")
        val latestTvShowPages =
            doc.selectFirst("#muvipro-posts-1")?.toHomePageList("Latest Tv Show")
        val sciFiPages = doc.selectFirst("#muvipro-posts-3")?.toHomePageList("Sci-Fi")
        val latestMoviesPages = doc.selectFirst("#gmr-main-load")?.toHomePageList("Latest Movie")

        return HomePageResponse(
            arrayListOf(
                recommendPages,
                latestTvShowPages,
                sciFiPages,
                latestMoviesPages
            ).filterNotNull()
        )
    }

    private fun Element.toHomePageList(name: String): HomePageList {
        val items = select(".gmr-item-modulepost, .gmr-slider-content, .gmr-box-content")
            .mapNotNull {
                it.toHomePageResult()
            }
        return HomePageList(name, items)
    }

    private fun Element.toHomePageResult(): SearchResponse? {
        val title = selectFirst(".gmr-slide-title, h2")?.text()?.trim() ?: return null
        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val img = selectFirst("img")
        val posterUrl = fixUrlNull(img?.attr("data-src")?.ifEmpty { img.attr("src") })

        return newAnimeSearchResponse(title, LoadUrl(href, posterUrl).toJson()) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query&post_type%5B%5D=post&post_type%5B%5D=tv"
        val document = app.get(url, referer = "$mainUrl/").document

        val items = document.select("#gmr-main-load article").mapNotNull {
            it.toHomePageResult()
        }
        return items
    }

    override suspend fun load(url: String): LoadResponse? {
        val d = tryParseJson<LoadUrl>(url) ?: return null
        val document = app.get(d.url, referer = "$mainUrl/").document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null

        val episodesElement = document.selectFirst(".gmr-listseries")

        val episodes = episodesElement?.select("a")?.map {
            newEpisode(it.attr("href")) {
                name = it.text()
            }
        } ?: arrayListOf(newEpisode(d.url) {
            name = title
        })

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            posterUrl = d.posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, referer = "$mainUrl/").document
        val id = doc.select("#muvipro_player_content_id").attr("data-id")
        val tabs = doc.select("#gmr-tab li a").map {
            it.attr("id")
        }
        val gdriveplayer = "https://pahe.sbs/wp-content/plugins/gdriveplayer/player.php?data="
        tabs.amap {
            val postData = mapOf(
                "action" to "muvipro_player_content",
                "tab" to it,
                "post_id" to id
            )
            val doc1 = app.post(
                "$mainUrl/wp-admin/admin-ajax.php",
                data = postData,
                referer = "$mainUrl/"
            ).document
            val link = doc1.select("iframe").attr("src").let { link ->
                if (link.startsWith(gdriveplayer)) {
                    "https:" + unescape(link.substringAfter(gdriveplayer))
                } else link
            }

//            Log.d("PaheProvider", link)
            loadExtractor(link, subtitleCallback, callback)
        }
        return true
    }

    data class LoadUrl(
        val url: String,
        val posterUrl: String?
    )

}
