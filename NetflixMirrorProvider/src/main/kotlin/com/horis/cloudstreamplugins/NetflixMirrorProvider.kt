package com.horis.cloudstreamplugins

import com.horis.cloudstreamplugins.entities.EpisodesData
import com.horis.cloudstreamplugins.entities.PlayList
import com.horis.cloudstreamplugins.entities.PostData
import com.horis.cloudstreamplugins.entities.SearchData
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Headers
import org.jsoup.nodes.Element

class NetflixMirrorProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "en"

    override var mainUrl = "https://m.netflixmirror.com"
    override var name = "NetflixMirror"

    override val hasMainPage = true
    private var time = ""

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val document = app.get("$mainUrl/home").document
        time = document.select("body").attr("data-time")
        val items = document.select(".tray-container, #top10").map {
            it.toHomePageList()
        }
        return HomePageResponse(items, false)
    }

    private fun Element.toHomePageList(): HomePageList {
        val name = select("h2, span").text()
        val items = select("article, .top10-post").mapNotNull {
            it.toSearchResult()
        }
        return HomePageList(name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val id = selectFirst("a")?.attr("data-post") ?: attr("data-post") ?: return null
        val posterUrl =
            fixUrlNull(selectFirst(".card-img-container img, .top10-img img")?.attr("data-src"))

        return newAnimeSearchResponse("", Id(id).toJson()) {
            this.posterUrl = posterUrl
            posterHeaders = mapOf("Referer" to "$mainUrl/")
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search.php?s=$query&t=$time"
        val data = app.get(url, referer = "$mainUrl/").parsed<SearchData>()

        return data.searchResult.map {
            newAnimeSearchResponse(it.t, Id(it.id).toJson()) {
                posterUrl = "https://i0.wp.com/img.netflixmirror.com/poster/v/${it.id}.jpg"
                posterHeaders = mapOf("Referer" to "$mainUrl/")
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val headers = mapOf(
            "X-Requested-With" to "XMLHttpRequest"
        )
        val id = parseJson<Id>(url).id
        val data = app.get(
            "$mainUrl/post.php?id=$id&t=$time", referer = "$mainUrl/", headers = headers
        ).parsed<PostData>()

        val episodes = arrayListOf<Episode>()

        val title = data.title

        if (data.episodes.first() == null) {
            episodes.add(newEpisode(LoadData(title, id)) {
                name = data.title
            })
        } else {
            data.episodes.filterNotNull().mapTo(episodes) {
                newEpisode(LoadData(title, it.id)) {
                    name = it.t
                    episode = it.ep.replace("E", "").toIntOrNull()
                    season = it.s.replace("S", "").toIntOrNull()
                }
            }

            if (data.nextPageShow == 1) {
                episodes.addAll(getEpisodes(title, url, data.nextPageSeason!!, 2))
            }

            data.season?.dropLast(1)?.amap {
                episodes.addAll(getEpisodes(title, url, it.id, 1))
            }
        }

        val type = if (data.episodes.first() == null) TvType.Movie else TvType.TvSeries

        return newTvSeriesLoadResponse(title, url, type, episodes) {
            posterUrl = "https://i0.wp.com/img.netflixmirror.com/poster/h/$id.jpg"
            posterHeaders = mapOf("Referer" to "$mainUrl/")
            plot = data.desc
            year = data.year.toIntOrNull()
        }
    }

    private suspend fun getEpisodes(
        title: String, eid: String, sid: String, page: Int
    ): List<Episode> {
        val episodes = arrayListOf<Episode>()
        var pg = page
        while (true) {
            val headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest"
            )
            val data = app.get(
                "$mainUrl/episodes.php?s=$sid&series=$eid&t=$time&page=$pg",
                referer = "$mainUrl/",
                headers = headers
            ).parsed<EpisodesData>()
            data.episodes?.mapTo(episodes) {
                newEpisode(LoadData(title, it.id)) {
                    name = it.t
                    episode = it.ep.replace("E", "").toIntOrNull()
                    season = it.s.replace("S", "").toIntOrNull()
                }
            }
            if (data.nextPageShow == 0) break
            pg++
        }
        return episodes
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val (title, id) = parseJson<LoadData>(data)
        val headers = mapOf(
            "X-Requested-With" to "XMLHttpRequest"
        )
        val playlist = app.get(
            "$mainUrl/playlist.php?id=$id&t=$title&tm=$time",
            referer = "$mainUrl/",
            headers = headers
        ).parsed<PlayList>()

        playlist.forEach { item ->
            item.sources.forEach {
                callback(
                    ExtractorLink(
                        name, it.label, fixUrl(it.file), "$mainUrl/", Qualities.Unknown.value, true
                    )
                )
            }
        }
        return true
    }

    data class Id(
        val id: String
    )

    data class LoadData(
        val title: String, val id: String
    )

}
