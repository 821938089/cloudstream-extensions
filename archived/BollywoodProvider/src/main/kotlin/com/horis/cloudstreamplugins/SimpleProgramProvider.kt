package com.horis.cloudstreamplugins

import android.annotation.SuppressLint
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SeasonData
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.base64DecodeArray
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.encode
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDateFormat")
class SimpleProgramProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )

    override var lang = "en"

    override var mainUrl = "https://bollywood.eu.org"
    override var name = "SimpleProgram"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "Trending" to "Trending Worldwide",
        "IN hi" to "Bollywood",
        "US en" to "Hollywood",
        "indian_tv" to "Indian TV",
        "english_tv" to "English TV",
        "IN hi" to "Hindi",
        "IN ta" to "Tamil",
        "IN te" to "Telugu",
        "IN kn" to "Kannada",
        "IN ml" to "Malayalam",
        "IN bn" to "Bengali",
        "IN pa" to "Punjabi",
        "IN mr" to "Marathi",
        "IN gu" to "Gujarati",
        "IN ur" to "Urdu",
        "CN zh" to "Mandarin Chinese",
        "ES es" to "Spanish",
        "EG ar" to "Arabic",
        "PT pt" to "Portuguese",
        "RU ru" to "Russian",
        "JP ja" to "Japanese",
        "DE de" to "German",
        "FR fr" to "French",
        "KR ko" to "Korean",
        "IT it" to "Italian"
    )

    private val todayStr by lazy { SimpleDateFormat("yyyy-MM-dd").format(Date()) }

    private val api = "https://simpleprogramdriveapi.zindex.eu.org"
    private val dbApi = "https://api.themoviedb.org/3"
    private val dbKey = "dadeb7117ea7ec03b1017935e5404812"
    private val imageApi = "https://i1.wp.com/image.tmdb.org/t/p/w500"

    private val clearRegex = "[()'\"@\$:]".toRegex()

    private var apiConfig: BollywoodProvider.ApiConfig? = null
        get() {
            field?.let {
                return it
            }
            field = runBlocking {
                getConfig()
            }
            return field
        }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val isTv = request.data.contains("_tv")

        val data = if (isTv) {
            val language = if (request.data == "indian_tv") "hi" else "en"
            app.get(
                "$dbApi/discover/tv" +
                        "?api_key=$dbKey" +
                        "&with_original_language=$language&page=$page" +
                        "&sort_by=popularity.desc&air_date.lte=$todayStr"
            ).parsed<TMDBData>().results.toSearchResponseList(true)
        } else if (request.data == "Trending") {
            app.get(
                "$dbApi/trending/all/day" +
                        "?api_key=$dbKey&page=$page"
            ).parsed<TMDBData>().results.map {
                newAnimeSearchResponse(
                    it.name ?: it.title!!,
                    LoadUrl(it.media_type == "tv", it).toJson()
                ) {
                    posterUrl =
                        "$imageApi/${it.poster_path ?: it.backdrop_path}"
                }
            }
        } else {
            val (region, language) = request.data.split(" ")
            app.get(
                "$dbApi/discover/movie" +
                        "?api_key=$dbKey&sort_by=popularity.desc" +
                        "&release_date.lte=$todayStr&region=$region" +
                        "&with_original_language=$language" +
                        "&page=$page"
            ).parsed<TMDBData>().results.toSearchResponseList(false)
        }

        return newHomePageResponse(request.name, data)
    }

    private fun List<TMDBResult>.toSearchResponseList(isTv: Boolean): List<SearchResponse> {
        return map {
            newAnimeSearchResponse(
                it.name ?: it.title!!,
                LoadUrl(isTv, it).toJson()
            ) {
                posterUrl = "$imageApi/${it.poster_path ?: it.backdrop_path}"
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val data: LoadUrl

        try {
            data = parseJson(url)
        } catch (e: Exception) {
            val file = parseJson<GDFile>(url)
            val title = file.name
            val episodes = listOf(newEpisode(file) {
                name = title
            })
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes)
        }

        val name = data.data.name ?: data.data.title!!

        val type = if (data.isTv) "tv" else "movie"
        val detail = app.get(
            "$dbApi/$type/${data.data.id}" +
                    "?api_key=$dbKey" +
                    "&append_to_response=credits%2Cimages%2Cvideos"
        ).parsed<TMDBResult>()

        var seasons: List<SeasonData>? = null

        val episodes = if (data.isTv) {
            seasons = detail.seasons!!.map {
                SeasonData(it.season_number, it.name)
            }
            seasons.amap { season ->
                app.get(
                    "$dbApi/tv/${data.data.id}/season/${season.season}" +
                            "?api_key=$dbKey" +
                            "&append_to_response=credits%2Cimages%2Cvideos"
                ).parsed<TMDBDetailEpisodes>().episodes.map {
                    newEpisode(LoadLinkData(name, it)) {
                        this.name = it.name
                        this.season = it.season_number
                        episode = it.episode_number
                        it.still_path?.let { path ->
                            posterUrl = "$imageApi$path"
                        }
                        description = it.overview
                    }
                }
            }.flatten()
        } else {
            val releaseYear = data.data.release_date?.substring(0, 4)
                ?: detail.release_date?.substring(0, 4)
                ?: ""

            val nameCleaned = name.replace(clearRegex, "")

            queryDriver("$nameCleaned $releaseYear").map {
                newEpisode(it) {
                    this.name = it.name
                }
            }
        }

        return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
            if (data.data.backdrop_path != null) {
                posterUrl =
                    "$imageApi/${data.data.poster_path ?: data.data.backdrop_path}"
            }
            plot = data.data.overview
            seasonNames = seasons
        }

    }

    override suspend fun search(query: String): List<SearchResponse>? = coroutineScope {
        val movie = async {
            app.get(
                "$dbApi/search/movie" +
                        "?api_key=$dbKey&page=1&sort_by=popularity.desc&query=$query"
            ).parsed<TMDBData>().results.toSearchResponseList(false)
        }
        val tv = async {
            app.get(
                "$dbApi/search/tv" +
                        "?api_key=$dbKey&page=1&sort_by=popularity.desc&query=$query"
            ).parsed<TMDBData>().results.toSearchResponseList(true)
        }
        val driver = async {
            queryDriver(query).toSearchResponseList()
        }
        return@coroutineScope movie.await() + tv.await() + driver.await()
    }

    private fun List<GDFile>.toSearchResponseList(): List<SearchResponse> {
        return map {
            newAnimeSearchResponse(it.name, it.toJson())
        }
    }

    private suspend fun queryDriver(query: String): List<GDFile> {
        //        val password = "simpleprogramdriveapi"
        //val key = base64Encode(((timestamp + 5 * 60 * 1000) / 1000).toString().toByteArray())
//        val auth = CryptoAES.encrypt(password, key)

        val iv = base64DecodeArray("FOdNRNRkiExjYiAgfMHgAg==") // encrypt_iv
        val timestamp = (System.currentTimeMillis() + 5 * 60 * 1000) / 1000
        val tsShort = timestamp.toString().toByteArray()
        val key = "1b73b8f245cd71f475403a1bce8f14eb".toByteArray()
        val auth = base64Encode(aesEncrypt(tsShort, key, iv))

        val headers = mapOf(
            "Authorization" to "Bearer $auth",
            "Referer" to "$mainUrl/"
        )

        val postData = mapOf(
            "query" to query,
            "timestamp" to System.currentTimeMillis()
        )

        val res = app.post(
            "$api/drive",
            headers = headers,
            requestBody = postData.toJson().toRequestBody("application/json".toMediaType())
        )

        return res.parse()
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        kotlin.runCatching {
            val file = parseJson<GDFile>(data)
            val link = generateLink(file)
            callback(
                ExtractorLink(
                    name,
                    name,
                    link,
                    "$mainUrl/",
                    Qualities.Unknown.value,
                )
            )
            return true
        }
        val loadData = parseJson<LoadLinkData>(data)
        val nameCleaned = loadData.name.replace(clearRegex, "")
        val season = "S${loadData.episode.season_number.toString().padStart(2, '0')}"
        val ep = "E${loadData.episode.episode_number.toString().padStart(2, '0')}"
        queryDriver("$nameCleaned $season$ep").forEach {
            val link = generateLink(it)
            callback(
                ExtractorLink(
                    name,
                    it.name,
                    link,
                    "$mainUrl/",
                    Qualities.Unknown.value,
                )
            )
        }
        return true
    }

    private fun generateLink(file: GDFile): String {
        val key1 = "1b73b8f245cd71f475403a1bce8f14eb".toByteArray()
        val iv = base64DecodeArray("FOdNRNRkiExjYiAgfMHgAg==")
        val fileId = String(aesDecrypt(base64DecodeArray(file.id), key1, iv))

        val key = "bhadoosystem"
        val expiry = (System.currentTimeMillis() + 345600000).toString()
        val hmacSign = "$fileId@$expiry".encode()
            .hmacSha256(key.encode()).base64().replace("+", "-")
        val encryptedId = base64Encode(CryptoAES.encrypt(key, fileId).toByteArray())
        val encryptedExpiry = base64Encode(CryptoAES.encrypt(key, expiry).toByteArray())
        val worker = apiConfig?.workers?.random()
            ?: throw ErrorLoadingException("worker not found.")
        val serviceName = apiConfig?.serviceName?.random()
            ?: throw ErrorLoadingException("serviceName not found.")
        return "https://$serviceName.$worker.workers.dev/download.aspx" +
                "?file=$encryptedId&expiry=$encryptedExpiry&mac=$hmacSign"
    }

    private suspend fun getConfig(): BollywoodProvider.ApiConfig {
        val regex = """const country = "(.*?)"
const time = ".*?"
var newtime = ".*?"
const downloadtime = "(.*?)"
const arrayofworkers = (.*)
const service_name = (.*)""".toRegex()
        val js = app.get(
            "https://geolocation.zindex.eu.org/api.js",
            referer = "$mainUrl/",
        ).text
        val match = regex.find(js) ?: throw ErrorLoadingException("parse api config fail")
        val country = match.groupValues[1]
        val downloadTime = match.groupValues[2]
        val workers = AppUtils.tryParseJson<List<String>>(match.groupValues[3])
            ?: throw ErrorLoadingException("parse api config fail (workers)")
        val serviceName = AppUtils.tryParseJson<List<String>>(match.groupValues[4])
            ?: throw ErrorLoadingException("parse api config fail (serviceName)")

        return BollywoodProvider.ApiConfig(country, downloadTime, workers, serviceName)
    }

    data class LoadUrl(
        val isTv: Boolean,
        val data: TMDBResult
    )

    data class LoadLinkData(
        val name: String,
        val episode: SingleEpisode
    )

    data class GDFile(
        val id: String,
        val mimeType: String,
        val name: String,
        val size: String
    )
}
