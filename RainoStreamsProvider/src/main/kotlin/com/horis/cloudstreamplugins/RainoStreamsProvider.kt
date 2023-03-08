package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import java.util.Calendar

class RainoStreamsProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Live
    )
    override var lang = "en"

    override var mainUrl = "http://rainostreams.com/"
    override var name = "RainoStreams"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "soccer" to "Soccer",
        "nfl" to "NFL",
        "ncaaf" to "NCAAF",
        "mlb" to "NCAAF",
        "nba" to "NBA",
        "nhl" to "NHL",
        "rugby.json" to "Rugby",
        "racing" to "Race",
        "cricket" to "Cricket",
        "mma" to "MMA",
    )

    private val api = "http://streamsapi.xyz/api/"
    private val rugbyApi = "http://api.bdnewszh.com/rugby.json"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val items = when (val type = request.data) {
            "rugby.json" -> {
                val events = app.get(rugbyApi, referer = "$mainUrl/").parsed<RugbyGames>().game
                events.mapNotNull {
                    it.toSearchResult()
                }
            }
            else -> {
                val events = app.get("$api$type", referer = "$mainUrl/").parsed<PEEvents>().events
                events.mapNotNull {
                    it.toSearchResult(type)
                }
            }
        }

        return newHomePageResponse(request.name, items)
    }

    private fun PEEvent.toSearchResult(type: String): SearchResponse? {
        val moment = getMoment()
        val stream = stream.ifEmpty { title.split(" ").last().lowercase() }
        val type2 = if (type == "racing") "f1" else type
        val url = "$mainUrl/$type2/$stream?moment=$moment&match=${title.replace(" ", "-")}"

        return newAnimeSearchResponse(title, Match(title, url).toJson())
    }

    private fun RugbyGame.toSearchResult(): SearchResponse? {
        val moment = getMoment()
        val title = "$home_team_name vs $away_team_name"
        val url = "$mainUrl/rugby/$stream?moment=$moment&match=$title"

        return newAnimeSearchResponse(title, Match(title, url).toJson())
    }

    private fun getMoment(): String {
        val calendar = Calendar.getInstance()
        val date = calendar.get(Calendar.DATE)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY) + 1
        return "$hour$date$month$year"
    }


    override suspend fun search(query: String): List<SearchResponse>? {
        return null
    }

    override suspend fun load(url: String): LoadResponse? {
        val match = parseJson<Match>(url)
        val doc = app.get(match.url, referer = "$mainUrl/").document
        val title = match.name
        val iframeSrc = "http:" + doc.select("iframe").attr("src")

        val episodes = arrayListOf(newEpisode(iframeSrc) {
            name = title
        })

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes)
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val html = app.get(data, verify = false).text
        val url = html.substring("{source: '", "'")
        callback(
            ExtractorLink(
                name,
                name,
                url,
                data.substring(0, data.indexOf("/", 8)) + "/",
                Qualities.Unknown.value,
                true
            )
        )
        return true
    }

    data class Match(
        val name: String,
        val url: String
    )

}
