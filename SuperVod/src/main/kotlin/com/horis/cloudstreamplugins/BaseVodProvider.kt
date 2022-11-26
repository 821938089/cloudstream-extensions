package com.horis.cloudstreamplugins

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import okhttp3.Response

abstract class BaseVodProvider : MainAPI() {

    companion object {
        const val TAG = "BaseVodProvider"
        val nsfwCategory = listOf("伦理")
    }

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Cartoon,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Documentary,
        TvType.Others
    )
    override var lang = "zh"
    override val hasMainPage = true
    override val mainPage = mutableListOf(MainPageData("", ""))

    open val apiExtractor by lazy { makeApiExtractor(mainUrl) }
    open val playFromFilter = hashSetOf("m3u8")
    open val headers = mapOf<String, String>()

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val categoryList = apiExtractor.getCategory().filter { cat ->
            nsfwCategory.any { !cat.typeName.contains(it) }
        }
        if (mainPage.first().name.isEmpty()) {
            makeMainPage(categoryList)
        }
        var pages = if (request.name.isNotEmpty()) {
            listOf(getSingleMainPage(page, request.name, request.data))
        } else {
            categoryList.amap {
                getSingleMainPage(page, it.typeName, it.typeId.toString())
            }
        }
        if (page == 1) {
            pages = pages.filter { it.list.isNotEmpty() }
        }
        return HomePageResponse(pages, pages.any { it.list.isNotEmpty() })
    }

    private fun makeMainPage(categoryList: List<Category>) {
        mainPage.clear()
        categoryList.forEach {
            mainPage.add(MainPageData(it.typeName, it.typeId.toString()))
        }
    }

    private suspend fun getSingleMainPage(page: Int, name: String, type: String): HomePageList {
        val pageSize = if (page == 1) 10 else null
        val vodList = apiExtractor.getVodListDetail(type = type, page = page, pageSize = pageSize)
            ?: throw ErrorLoadingException("获取主页数据失败")
        val homeList = ArrayList<SearchResponse>()
        for (vod in vodList) {
            vod.name ?: continue
            homeList.add(newMovieSearchResponse(vod.name, vod.toJson()) {
                posterUrl = vod.pic
            })
        }
        return HomePageList(name, homeList)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun search(query: String): List<SearchResponse>? {
        val vodList = if (query.split(",").all { it.toIntOrNull() != null }) {
            apiExtractor.getVodListDetail(ids = query)
        } else {
            apiExtractor.getVodListDetail(query)
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