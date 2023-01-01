package com.horis.cloudstreamplugins

import android.annotation.SuppressLint
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.nicehttp.NiceResponse
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Response
import okio.ByteString.Companion.encode
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
class NivodProvider : MainAPI() {
    companion object {
        const val QUERY_PREFIX = "__QUERY::"
        const val BODY_PREFIX = "__BODY::"
        const val SECRET_PREFIX = "__KEY::"
        const val HOST_CONFIG_KEY = "2x_Give_it_a_shot"
    }

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    override var mainUrl = "https://www.nivod.tv"
    override var name = "泥视频"

    override val hasMainPage = true

    private val apiUrl = "https://api.nivod.tv"
    private val decryptInterceptor by lazy { DecryptInterceptor() }
    private val todayStr by lazy { SimpleDateFormat("yyyy-MM-dd").format(Date()) }
    private var oid = ""
    private var cookies: Map<String, String> = mapOf()

    override var sequentialMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (oid.isEmpty()) {
            initUserIdentity()
        }
        val niVodHome = getNiVodHome()

        val items = niVodHome.list.map {
            it.toHomePageList()
        }
        return HomePageResponse(items, false)
    }

    private fun Section.toHomePageList(): HomePageList {
        val items = rows.map { it.cells }.flatten().mapNotNull {
            it.toSearchResult()
        }
        return HomePageList(title, items)
    }

    private fun Cell.toSearchResult(): SearchResponse? {
        return newAnimeSearchResponse(title, this.toJson()) {
            posterUrl = img
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun search(query: String): List<SearchResponse>? {
        val postData = mapOf(
            "keyword" to query,
            "start" to "0",
            "cat_id" to "1",
            "keyword_type" to "0"
        )
        val search = request("$apiUrl/show/search/WEB/3.2", postData).parsed<SearchResp>()
        val items = search.list.mapNotNull {
            it.toSearchResult()
        }
        return items
    }

    private fun SearchEntity.toSearchResult(): SearchResponse? {
        return newAnimeSearchResponse(showTitle, this.toJson()) {
            posterUrl = showImg
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val showIdCode = tryParseJson<Cell>(url)?.show?.showIdCode
            ?: parseJson<SearchEntity>(url).showIdCode
        val postData = mapOf("show_id_code" to showIdCode)
        val detail = request("$apiUrl/show/detail/WEB/3.2", postData).parsed<DetailResponse>()
        val entity = detail.entity
        val name = entity.showTitle
        val episodes = detail.entity.plays.map {
            it.showIdCode = entity.showIdCode
            newEpisode(it) {
                this.name = it.displayName
            }
        }
        return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
            posterUrl = entity.showImg
            plot = entity.showDesc
            actors = entity.actors.let {
                if (it.isEmpty()) null else
                    it.split(",").map { name -> ActorData(Actor(name)) }
            }
            year = entity.postYear.toIntOrNull()
            tags = arrayListOf(entity.showTypeName, entity.regionName)
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val play = parseJson<Play>(data)
        val postData = mapOf(
            "show_id_code" to play.showIdCode!!,
            "play_id_code" to play.playIdCode,
            "oid" to "1"
        )
        val playInfo = request("$apiUrl/show/play/info/WEB/3.2", postData)
            .parsed<PlayInfoResponse>()

        val link = playInfo.entity.playUrl

        callback(
            ExtractorLink(
                name,
                name,
                link,
                "",
                Qualities.Unknown.value,
                link.contains(".m3u8")
            )
        )

        return true
    }

    @Suppress("ObjectLiteralToLambda")
    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request().newBuilder().removeHeader("referer").build()
                return chain.proceed(request)
            }
        }
    }

    private suspend fun initUserIdentity() {
        val res = request("$apiUrl/user/identity/init/WEB/3.2")
        if (res.code == 403) {
            error("你所在的地区无法访问网站，该网站仅海外可用。")
        }
        oid = res.parsed<UserIdentity>().oid
        cookies = mapOf(
            "oid" to oid,
            "new_user" to todayStr
        )
    }

    private suspend fun getNiVodHome(): NiVodHome {
        val res = request("$apiUrl/index/desktop/WEB/3.3", mapOf("start" to "0"))
        return res.parsed()
    }

//    private suspend fun getApiConfig() {
//        val res = request("$apiUrl/global/config/WEB/3.2")
//    }

    private suspend fun request(
        url: String,
        data: Map<String, String> = mapOf(),
    ): NiceResponse {
        if (oid.isEmpty() && !url.contains("/user/identity/init/WEB/3.2")) {
            initUserIdentity()
        }

        val time = System.currentTimeMillis()
        val defaultQuery = mapOf(
            "_ts" to "$time",
            "app_version" to "1.0",
            "platform" to "3",
            "market_id" to "web_nivod",
            "device_code" to "web",
            "versioncode" to "1",
            "oid" to oid
        )
        val sign = createSign(defaultQuery, data)
        val body = FormBody.Builder().run {
            data.forEach {
                add(it.key, it.value)
            }
            build()
        }
        val defaultParams = "?_ts=$time&app_version=1.0&platform=3&market_id=web_nivod" +
                "&device_code=web&versioncode=1&oid=$oid&sign=$sign"
        return app.post(
            "$url$defaultParams",
            referer = "$mainUrl/",
            requestBody = body,
            cookies = cookies,
            interceptor = decryptInterceptor
        )
    }

    @Suppress("UnnecessaryVariable")
    private fun createSign(
        queryMap: Map<String, String>,
        bodyMap: Map<String, String> = mapOf(),
        key: String = HOST_CONFIG_KEY
    ): String {
        val query = queryMap.entries.sortedBy { it.key }
            .fold(QUERY_PREFIX) { last, (key, value) ->
                if (key.isEmpty() || value.isEmpty() || key == "sign") last else
                    "$last$key=$value&"
            }
        val body = bodyMap.entries.sortedBy { it.key }
            .fold(BODY_PREFIX) { last, (key, value) ->
                if (key.isEmpty() || value.isEmpty() || key == "sign") last else
                    "$last$key=$value&"
            }
        val result = "$query$body$SECRET_PREFIX$key".encode().md5().hex()
        return result
    }

    private fun error(msg: String = "加载数据失败"): Nothing {
        throw ErrorLoadingException(msg)
    }

}
