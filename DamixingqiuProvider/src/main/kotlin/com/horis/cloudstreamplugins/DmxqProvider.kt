package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.toByteString
import org.jsoup.nodes.Element
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DmxqProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    // https://www.dmys.tv/
    override var mainUrl = "https://www.dmdy4.vip"
    override var name = "大米星球"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/vodshow/20--------" to "电影",
        "${mainUrl}/vodshow/21--------" to "电视剧",
        "${mainUrl}/vodshow/22--------" to "动漫",
        "${mainUrl}/vodshow/23--------" to "综艺",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page == 1) {
            "${request.data}---.html"
        } else {
            "${request.data}$page---.html"
        }
        val document = app.get(url, referer = "$mainUrl/").document
        val items = document.select(".module-items > a").mapNotNull {
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
                fixUrlNull(document.selectFirst(".module-item-pic img")?.attr("src"))
            tags = document.select(".module-info-tag a").map { it.text().trim() }
            plot = document.select(".module-info-introduction").text().trim()
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
        if (vod.encrypt != 3) {
            return false
        }
        val playUrl = vod.getPlayUrl()?.let {
            getPlayUrl(it)
        } ?: return false
        callback(
            ExtractorLink(
                name,
                vod.playerInfo?.from ?: "",
                playUrl,
                "",
                Qualities.Unknown.value,
                true
            )
        )
        return true
    }

    private suspend fun getPlayUrl(url: String): String? {
        val sign = url.encode().hmacSha256("55ca5c4d11424dcecfe16c08a943afdc".encode()).hex()
        val payload = """{"type":"getUrl","url":"$url","sign":"$sign"}""".toByteArray()
        val key = "55ca5c48a943afdc".toByteArray()
        val iv = "d11424dcecfe16c0".toByteArray()
        val encryptedData = aesEncrypt(payload, key, iv).toHex().uppercase().toByteArray()
        val server = arrayListOf(
            "wss://player.sakurot.com:3458/wss",
            "wss://player2.lscsfw.com:6723/wss"
        ).random()

        return suspendCancellableCoroutine { cout ->
            ioSafe {
                try {
                    val websocket = createWebSocket(server, object : WebSocketListener() {
                        override fun onMessage(webSocket: WebSocket, text: String) {
                            val data = text.decodeHex().toByteArray()
                            val decryptedData = String(aesDecrypt(data, key, iv))
                            val playUrl = tryParseJson<PlayUrl>(decryptedData)?.url
                            webSocket.cancel()
                            if (cout.isActive) {
                                cout.resume(playUrl)
                            }
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?
                        ) {
                            if (cout.isActive) {
                                cout.resumeWithException(t)
                            }
                        }
                    })
                    websocket.send(encryptedData.toByteString())
                } catch (e: Exception) {
                    if (cout.isActive) {
                        cout.resumeWithException(e)
                    }
                }
            }
        }
    }

    @Suppress("ObjectLiteralToLambda")
    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request().newBuilder().removeHeader("referer").build())
            }
        }
    }

    data class PlayUrl(
        @JsonProperty("url") val url: String
    )

}
