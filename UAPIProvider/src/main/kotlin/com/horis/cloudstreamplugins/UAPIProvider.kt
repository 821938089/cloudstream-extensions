package com.horis.cloudstreamplugins

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.nicehttp.NiceResponse
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLEncoder

abstract class UAPIProvider : MainAPI() {

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

    var categoryCache: List<Category>? = null

    private suspend fun getCategory(): List<Category> {
        categoryCache?.let { return it }
        categoryCache =
            fetchApi("$mainUrl/provide/vod/?ac=list").parsedSafe<CategoryList>()?.list?.take(8)
        return categoryCache ?: throw ErrorLoadingException("获取分类数据失败")
    }

    private suspend fun fetchApi(url: String): NiceResponse {
        var retry = 2
        while (retry-- > 0) {
            try {
                return app.get(url, referer = url, verify = false)
            } catch (_: Exception) {
            }
        }
        return app.get(url, referer = url, verify = false)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val category = getCategory()

        var pages = category.amap {
            val vodList =
                fetchApi("$mainUrl/provide/vod/?ac=detail&t=${it.typeId}&pg=$page")
                    .parsedSafe<VodList>()?.list ?: throw ErrorLoadingException("获取主页数据失败")
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
        val encodeQuery = URLEncoder.encode(query, "utf-8")
        val vodList =
            fetchApi("$mainUrl/provide/vod/?ac=detail&wd=$encodeQuery")
                .parsedSafe<VodList>()?.list ?: throw ErrorLoadingException("获取搜索数据失败")
        return vodList.mapNotNull {
            it.name ?: return@mapNotNull null
            newTvSeriesSearchResponse(it.name, it.toJson()) {
                posterUrl = it.pic
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val vod = parseJson<Vod>(url)
        vod.name ?: return null
        val episodes = ArrayList<Episode>()
        val serverNames = ArrayList<SeasonData>()
        val servers = vod.playFrom!!.split("$$$")

        for ((index, vodPlayList) in vod.playUrl!!.split("$$$").withIndex()) {
            serverNames.add(SeasonData(index + 1, servers[index]))
            for (playInfo in vodPlayList.trimEnd('#').split("#")) {
                val (episodeName, playUrl) = playInfo.split("$")
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
        val playData = parseJson<PlayData>(data)
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
                return chain.proceed(chain.request().newBuilder().removeHeader("referer").build())
            }
        }
    }

    data class PlayData(
        val server: String,
        val url: String
    )

    data class VodList(
        @JsonProperty("list") val list: ArrayList<Vod>
    )

    data class Vod(
        @JsonProperty("vod_id") val id: Int? = null,
        @JsonProperty("type_id") val typeId: Int? = null,
        @JsonProperty("vod_name") val name: String? = null,
        @JsonProperty("vod_sub") val sub: String? = null,
        @JsonProperty("vod_pic") val pic: String? = null,
        @JsonProperty("vod_actor") val actor: String? = null,
        @JsonProperty("vod_director") val director: String? = null,
        @JsonProperty("vod_writer") val writer: String? = null,
        @JsonProperty("vod_blurb") val blurb: String? = null, // 简介
        @JsonProperty("vod_remarks") val remarks: String? = null,
        @JsonProperty("vod_area") val area: String? = null,
        @JsonProperty("vod_lang") val lang: String? = null,
        @JsonProperty("vod_year") val year: String? = null,
        @JsonProperty("vod_time") val time: String? = null,
        @JsonProperty("vod_content") val content: String? = null,
        @JsonProperty("vod_play_from") val playFrom: String? = null,
        @JsonProperty("vod_play_server") val playServer: String? = null,
        @JsonProperty("vod_play_note") val playNote: String? = null,
        @JsonProperty("vod_play_url") val playUrl: String? = null,
        @JsonProperty("type_name") val typeName: String? = null,
    )

    data class CategoryList(
        @JsonProperty("class") val list: ArrayList<Category>
    )

    data class Category(
        @JsonProperty("type_id") val typeId: Int,
        @JsonProperty("type_pid") val typePid: Int,
        @JsonProperty("type_name") val typeName: String
    )

}
