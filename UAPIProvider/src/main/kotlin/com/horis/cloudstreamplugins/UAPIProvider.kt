package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.nicehttp.NiceResponse
import okhttp3.Interceptor
import okhttp3.Response

abstract class UAPIProvider : MainAPI() {

    companion object {
        const val UserAgent =
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36"
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

    var categoryCache: ArrayList<Category>? = null

    private suspend fun getCategory(): ArrayList<Category> {
        categoryCache?.let { return it }
        categoryCache = fetchApi("$mainUrl/provide/vod/?ac=list").parsedSafe<CategoryList>()?.list
        return categoryCache ?: throw ErrorLoadingException("获取分类数据失败")
    }

    private suspend fun fetchApi(url: String): NiceResponse {
        return app.get(url, referer = url, verify = false)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val category = getCategory()
        val pages = if (request.name == "") {
            category.amap {
                getSingleMainPage(page, it.typeId, it.typeName)
            }
        } else {
            arrayListOf(getSingleMainPage(page, request.name))
        }
        return HomePageResponse(pages, true)
    }

    private suspend fun getSingleMainPage(page: Int, name: String): HomePageList {
        val category = getCategory()
        val typeId = category.find { it.typeName == name }!!.typeId
        return getSingleMainPage(page, typeId, name)
    }

    private suspend fun getSingleMainPage(page: Int, typeId: Int, name: String): HomePageList {
        val vodList =
            fetchApi("$mainUrl/provide/vod/?ac=detail&t=${typeId}&pg=$page")
                .parsedSafe<VodList>()?.list ?: throw ErrorLoadingException("获取主页数据失败")
        val homeList = ArrayList<SearchResponse>()
        for (vod in vodList) {
            vod.name ?: continue
            homeList.add(newMovieSearchResponse(vod.name, vod.toJson()) {
                posterUrl = vod.pic
            })
        }
        return HomePageList(name, homeList)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val vodList =
            fetchApi("$mainUrl/provide/vod/?ac=detail&wd=$query")
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
            for (playInfo in vodPlayList.split("#")) {
                val (episodeName, playUrl) = playInfo.split("$")
                episodes.add(newEpisode("${servers[index]}$$playUrl") {
                    name = episodeName
                })
            }
        }

        return newTvSeriesLoadResponse(vod.name, url, TvType.TvSeries, episodes) {
            seasonNames = serverNames
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val (serverName, playUrl) = data.split("$")
        if (!serverName.contains("m3u8")) {
            throw ErrorLoadingException("请在浏览器中打开链接")
        }
        M3u8Helper.generateM3u8(name, playUrl, "", name = serverName).forEach(callback)
        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request().newBuilder().removeHeader("referer").build())
            }
        }
    }

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