package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.nicehttp.Session
import org.jsoup.nodes.Element
import java.util.Calendar
import kotlin.math.roundToInt

class Stream1Provider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Live
    )
    override var lang = "en"

    override var mainUrl = "https://1stream.eu"
    override var name = "1Stream"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "/" to "Home",
        "/nbastreams" to "NBA streams",
        "/nflstreams" to "NFL streams",
        "/nhlstreams" to "NHL streams",
        "/mlbstreams" to "MLB streams",
        "/mmastreams" to "MMA streams",
        "/boxingstreams" to "Boxing streams",
        "/f1streams" to "Formula 1 Streams"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val doc = app.get("$mainUrl${request.data}").document
        val items = doc.select("a.btn").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, items, false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = select("h4").text()
        val url = attr("href")
        val time = select(".media-body p").text()

        return newAnimeSearchResponse(title, Match(title, url, time).toJson()) {
            posterUrl = select("img").attr("src")
        }
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        return null
    }

    override suspend fun load(url: String): LoadResponse? {
        val match = parseJson<Match>(url)
        val title = match.name

        val episodes = arrayListOf(newEpisode(match.url) {
            name = title
        })

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            plot = match.time
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val html = session.get(data, verify = false).text
        val url = mainUrl +
                html.substring("url: '", "'") +
                (Math.random() * 64).roundToInt().toString()
        val eventId = html.substring("eventId: \"", "\"")
        val token = html.substring("\"_token\": \"", "\"")
        val sport = html.substring("sport: '", "'")
        val postData = mapOf(
            "eventId" to eventId,
            "_token" to token,
            "sport" to sport
        )
        val headers = mapOf(
            "X-Requested-With" to "XMLHttpRequest",
            "Referer" to data
        )
        val stream = session.post(url, headers = headers, data = postData).parsed<StreamInfo>()

        if (stream.baseurl.isEmpty()) {
            return false
        }

        val m3u8 = base64Decode(stream.source)

        callback(
            ExtractorLink(
                name,
                name,
                m3u8,
                "$mainUrl/",
                Qualities.Unknown.value,
                true
            )
        )
        return true
    }

    data class StreamInfo(
        val baseurl: String,
        val source: String
    )

    data class Match(
        val name: String,
        val url: String,
        val time: String
    )

}
