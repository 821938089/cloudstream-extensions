package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
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
            fetchApi("$mainUrl/provide/vod/?ac=list&t=${typeId}&pg=$page")
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