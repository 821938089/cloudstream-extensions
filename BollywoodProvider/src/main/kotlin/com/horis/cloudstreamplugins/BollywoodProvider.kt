package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okio.ByteString.Companion.encode
import java.util.concurrent.ConcurrentHashMap

class BollywoodProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "hi"

    override var mainUrl = "https://bollywood.eu.org"
    override var name = "Bollywood"

    override val hasMainPage = true

    private val api = "https://simpleprogramapi.zindex.eu.org"
    private val apiConfig by lazy {
        runBlocking {
            getConfig()
        }
    }

    override val mainPage = mainPageOf(
        "$api/0:/Bollywood.Hindi/" to "Bollywood Hindi Movies",
        "$api/0:/Hollywood.Hindi/" to "Hollywood Hindi Movies",
        "$api/0:/South.Indian.Hindi/" to "South Indian Hindi Movies",
        "$api/0:/Web.Series.Hindi/" to "Web Series Hindi",
        "$api/0:/Kids.Zone.Hindi/" to "Kids Zone Hindi Movies",
        "$api/3:/" to "Punjabi Movies"
    )

    private val nextPageToken = ConcurrentHashMap<String, String>()

    private val headers = mapOf(
        "Referer" to "$mainUrl/",
        "Origin" to mainUrl,
        "cf_cache_token" to "UKsVpQqBMxB56gBfhYKbfCVkRIXMh42pk6G4DdkXXoVh7j4BjV"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url =
            "${request.data}?password=&page_token=${nextPageToken[request.name] ?: ""}&page_index=${page - 1}&_=${System.currentTimeMillis()}"

        val gdIndex = app.get(url, headers = headers).parsedSafe<GDIndex>()
            ?: throw ErrorLoadingException("parse index data fail (mainpage)")
        gdIndex.nextPageToken?.let { nextPageToken[request.name] = it }
        gdIndex.data.files.forEach { it.parentFolder = request.data }

        val items = gdIndex.data.files.toSearchResponseList()

        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val url = "$api//0:search?q=$query&page_token=&page_index=0"
        val gdIndex = app.get(url, headers = headers).parsedSafe<GDIndex>()
            ?: throw ErrorLoadingException("parse index data fail (search)")

        return gdIndex.data.files.toSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val file = tryParseJson<GDFile>(url) ?: return null
        val title = file.name

        var seasons: List<SeasonData>? = null

        val episodes = if (file.isFolder) {
            val items = listDir(file)
            val folders = items.filter { it.isFolder }
            val files = items.filter { !it.isFolder }
            seasons = folders.mapIndexed { i, f -> SeasonData(i, f.name) }
            folders.amapIndexed { index, gdFile ->
                listDir(gdFile).map {
                    newEpisode(it) {
                        name = it.name
                        season = index
                    }
                }
            }.flatten().toMutableList().also {
                files.mapTo(it) { gdFile ->
                    newEpisode(gdFile) {
                        name = gdFile.name
                    }
                }
            }
        } else {
            arrayListOf(newEpisode(file) {
                name = title
            })
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            posterUrl =
                "https://i1.wp.com/image-cdn-simple-program.hashhackers.com/0:/images/${file.name}.png"
            seasonNames = seasons
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val file = parseJson<GDFile>(data)
        val key = "bhadoosystem"
        val expiry = (System.currentTimeMillis() + 345600000).toString()
        val hmacSignId = "${file.id}@$expiry".encode()
            .hmacSha256(key.encode()).base64().replace("+", "-")
        val encryptedId = base64Encode(CryptoJs.aesEncrypt(file.id, key).toByteArray())
        val encryptedExpiry = base64Encode(CryptoJs.aesEncrypt(expiry, key).toByteArray())
        val link =
            "https://api.${apiConfig.workers.random()}.workers.dev/public.php?file=$encryptedId&expiry=$encryptedExpiry&mac=$hmacSignId"
        callback(
            ExtractorLink(
                name,
                name,
                link,
                "",
                Qualities.Unknown.value,
            )
        )
        return true
    }

    private fun List<GDFile>.toSearchResponseList(): List<SearchResponse> {
        return map {
            newAnimeSearchResponse(it.name, it.toJson()) {
                posterUrl =
                    "https://i1.wp.com/image-cdn-simple-program.hashhackers.com/0:/images/${it.name}.png"
            }
        }
    }

    private suspend fun getConfig(): ApiConfig {
        val regex = """const country = "(.*?)";
const downloadtime = "(.*?)";
var arrayofworkers = (.*)""".toRegex()
        val js = app.get(
            "https://geolocation.zindex.eu.org/api.js",
            referer = "$mainUrl/",
        ).text
        val match = regex.find(js)
        val country =
            match?.groupValues?.get(1)
                ?: throw ErrorLoadingException("parse api config fail (country)")
        val downloadTime =
            match.groupValues[2]
        val workers = tryParseJson<List<String>>(match.groupValues[3])
            ?: throw ErrorLoadingException("parse api config fail (workers)")

        return ApiConfig(country, downloadTime, workers)
    }

    private suspend fun listDir(file: GDFile): List<GDFile> {
        var nextPageToken = ""
        var page = 0
        val files = arrayListOf<GDFile>()
        while (true) {
            val url =
                "${file.parentFolder}${file.name}/?password=&page_token=$nextPageToken&page_index=$page&_=${System.currentTimeMillis()}"
            val gdIndex = app.get(url, headers = headers).parsedSafe<GDIndex>()
                ?: throw ErrorLoadingException("parse index data fail (listdir)")
            gdIndex.data.files.forEach { it.parentFolder = "${file.parentFolder}${file.name}/" }
            files.addAll(gdIndex.data.files)
            nextPageToken = gdIndex.nextPageToken ?: break
            page++
        }
        return files
    }

    @Suppress("ObjectLiteralToLambda")
    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()
                    .newBuilder()
                    .removeHeader("referer")
                    .build()
                return chain.proceed(request)
            }
        }
    }

    data class GDIndex(
        val data: GDIndexData,
        val nextPageToken: String?,
        val curPageIndex: Int
    )

    data class GDIndexData(
        val files: List<GDFile>,
        val nextPageToken: String?
    )

    data class GDFile(
        val id: String,
        val mimeType: String?,
        val modifiedTime: String?,
        val name: String,
        val size: String?,
        var parentFolder: String?
    ) {
        val isFolder get() = mimeType == "application/vnd.google-apps.folder"
    }

    data class ApiConfig(
        val country: String,
        val downloadTime: String,
        val workers: List<String>
    )

}
