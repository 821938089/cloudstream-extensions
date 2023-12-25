package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.nicehttp.NiceResponse
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLEncoder

abstract class BaseUAPIProvider : MainAPI() {
    companion object {
        const val UserAgent =
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36"
        const val TAG = "UAPIProvider"
    }

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Documentary,
        TvType.Others
    )
    override var lang = "zh"
    override val hasMainPage = true

    open val playFromFilter = hashSetOf("m3u8")
    open val headers = mapOf<String, String>()

    var categoryCache: List<Category>? = null

    abstract suspend fun getCategory(): List<Category>

    abstract suspend fun getVodList(url: String): List<Vod>?

    suspend fun fetchApi(url: String): NiceResponse {
        var retry = 2
        while (retry-- > 0) {
            try {
                return app.get(url, headers, referer = url, verify = false)
            } catch (_: Exception) {
            }
        }
        return app.get(url, headers, referer = url, verify = false)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val category = getCategory()

        var pages = category.amap {
            val vodList = getVodList("$mainUrl?ac=videolist&t=${it.typeId}&pg=$page")
                ?: throw ErrorLoadingException("获取主页数据失败")
            val homeList = ArrayList<SearchResponse>()
            for (vod in vodList) {
                vod.name ?: continue
                homeList.add(newMovieSearchResponse(vod.name, vod.toJson()) {
                    posterUrl = vod.pic
                })
            }
            HomePageList(it.typeName, homeList)
        }
        if (page == 1) {
            pages = pages.filter { it.list.isNotEmpty() }
        }
        return HomePageResponse(pages, pages.any { it.list.isNotEmpty() })
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun search(query: String): List<SearchResponse>? {
        val vodList = if (query.split(",").all { it.toIntOrNull() != null }) {
            getVodList("$mainUrl?ac=videolist&ids=$query")
        } else {
            val encodeQuery = URLEncoder.encode(query, "utf-8")
            getVodList("$mainUrl?ac=videolist&wd=$encodeQuery")
        }
        vodList ?: throw ErrorLoadingException("获取搜索数据失败")
        return vodList.mapNotNull {
            it.name ?: return@mapNotNull null
            newTvSeriesSearchResponse(it.name, it.toJson()) {
                posterUrl = it.pic
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val vod = AppUtils.parseJson<Vod>(url)
        vod.name ?: return null
        val episodes = ArrayList<Episode>()
        val serverNames = ArrayList<SeasonData>()
        val servers = vod.playFrom!!.split("$$$")

        loop@ for ((index, vodPlayList) in vod.playUrl!!.split("$$$").withIndex()) {
//            if (playFromFilter.isNotEmpty() &&
//                !playFromFilter.any { servers[index].contains(it) }
//            ) {
//                continue
//            }
            serverNames.add(SeasonData(index + 1, servers[index]))
            for (playInfo in vodPlayList.trimEnd('#').split("#")) {
                val (episodeName, playUrl) = playInfo.split("$")
                if (!playInfo.endsWith(".m3u8")) {
                    serverNames.removeLast()
                    continue@loop
                }
                val data = PlayData(servers[index], playUrl).toJson()
                episodes.add(newEpisode(data) {
                    name = episodeName
                    season = index + 1
                })
            }
        }

        return newTvSeriesLoadResponse(vod.name, url, TvType.TvSeries, episodes) {
            seasonNames = serverNames
            posterUrl = vod.pic
            plot = vod.blurb ?: vod.content
            year = vod.year?.toInt()
            if (vod.actor!!.isNotBlank()) {
                actors = vod.actor.split(",").map { ActorData(Actor(it)) }
            }
            tags = arrayListOf(
                vod.area!!,
                vod.lang!!,
                vod.typeName!!,
                vod.remarks!!
            ).filter { it.isNotBlank() }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val playData = AppUtils.parseJson<PlayData>(data)
        callback(
            ExtractorLink(
                name,
                playData.server,
                playData.url,
                "",
                Qualities.Unknown.value,
                true
            )
        )
        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request().newBuilder().run {
                    removeHeader("referer")
//                    removeHeader("Accept-Encoding")
//                    headers.forEach { (k, _) ->
//                        if (k.startsWith("sec-")) {
//                            removeHeader(k)
//                        }
//                    }
                    build()
                }
                return chain.proceed(request)
            }
        }
    }
}