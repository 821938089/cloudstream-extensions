package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import okhttp3.Headers
import org.jsoup.nodes.Element

class Movie123Provider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "en"

    override var mainUrl = "https://ww1.new-movies123.co"
    override var name = "Movie123"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "$mainUrl/latest" to "NEWEST MOVIES AND TV SHOWS",
        "$mainUrl/all-movies" to "MOVIES",
        "$mainUrl/all-tv-series" to "TV-SERIES",
        "$mainUrl/top-imdb" to "IMDB BEST MOVIES AND TV SERIES",
        "$mainUrl/top-watched" to "WATCHED FREE MOVIES & TV SHOWS",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data}/$page/"
        }

        val document = app.get(url, referer = "$mainUrl/").document
        val items = document.select(".EwDalIbaMM > div").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("._watchOnline p")?.text()?.trim() ?: return null
        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(selectFirst("img")?.attr("data-src"))

        return newAnimeSearchResponse(title, href) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search/$query"
        val document = app.get(url, referer = "$mainUrl/").document

        return document.select(".EwDalIbaMM > div").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, referer = "$mainUrl/").document
        val title = document.selectFirst("#info h1")?.text()?.trim() ?: return null

        val episodes = document.select("#iDgkXUZslQ a").map {
            val epsName = it.text()
            val epsNumber = Regex("(?i)Episode\\s?(\\d+)").find(epsName)?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
            val epsSeason = Regex("(?i)Season\\s?(\\d+)").find(title)?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: 1
            newEpisode(it.attr("href")) {
                name = epsName
                episode = epsNumber
                season = epsSeason
            }
        }.let {
            it.ifEmpty {
                listOf(newEpisode(document.select(".dqxhQvqokS a").attr("href")) {
                    name = title
                })
            }
        }.distinctBy { it.name }

        val type = if (title.contains("Season", true)) TvType.TvSeries else TvType.Movie

        return newTvSeriesLoadResponse(title, url, type, episodes) {
            posterUrl = document.select(".XIUyJddvQj img").attr("data-src")
            plot = document.select(".IbKbFmtgpQ.tZvUJSGrNY").text()
            year = document.select(".CWuyleLaBP a").text().toIntOrNull()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = app.get(data, verify = false)
        val match = "var url = '(/user/servers/.*?\\?ep=.*?)';".toRegex().find(res.text)
        val serverUrl = match?.groupValues?.get(1) ?: return false
        val cookies = res.okhttpResponse.headers.getCookies()
        val doc = res.document
        val url = doc.select("meta[property=og:url]").attr("content")
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val qualities = intArrayOf(2160, 1440, 1080, 720, 480, 360)
        app.get(
            "$mainUrl$serverUrl",
            cookies = cookies, referer = url, headers = headers
        ).document.select("ul li").amap { el ->
            val server = el.attr("data-value")
            val encryptedData = app.get(
                "$url?server=$server&_=${System.currentTimeMillis()}",
                cookies = cookies,
                referer = url,
                headers = headers
            ).text
            val json = base64Decode(encryptedData).xorDecrypt()
            val links = tryParseJson<List<VideoLink>>(json) ?: return@amap
            links.forEach { video ->
                qualities.filter { it <= video.max.toInt() }.forEach {
                    callback(
                        ExtractorLink(
                            name,
                            video.language,
                            video.src.split("360", limit = 3).joinToString(it.toString()),
                            "$mainUrl/",
                            it
                        )
                    )
                }
            }
        }
        return true
    }

    private fun String.xorDecrypt(key: String = "124"): String {
        val sb = StringBuilder()
        var i = 0
        while (i < this.length) {
            var j = 0
            while (j < key.length && i < this.length) {
                sb.append((this[i].code xor key[j].code).toChar())
                j++
                i++
            }
        }
        return sb.toString()
    }

    private fun Headers.getCookies(cookieKey: String = "set-cookie"): Map<String, String> {
        // Get a list of cookie strings
        // set-cookie: name=value; -----> name=value
        val cookieList =
            this.filter { it.first.equals(cookieKey, ignoreCase = true) }.mapNotNull {
                it.second.split(";").firstOrNull()
            }

        // [name=value, name2=value2] -----> mapOf(name to value, name2 to value2)
        return cookieList.associate {
            val split = it.split("=", limit = 2)
            (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
        }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
    }

    data class VideoLink(
        val src: String,
        val file: String,
        val label: Int,
        val type: String,
        val size: String,
        val max: String,
        val language: String
    )

}
