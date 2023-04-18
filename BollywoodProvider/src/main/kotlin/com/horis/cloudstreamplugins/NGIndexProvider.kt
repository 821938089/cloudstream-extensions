package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLDecoder

class NGIndexProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "en"

    override var mainUrl = "http://103.213.238.85:8090"
    override var name = "NGIndex"

    override val hasMainPage = true

    private val headers = mapOf(
        "Referer" to "$mainUrl/",
    )

    private var rootPageData = emptyList<NGFile>()

    private val cacheFiles = hashMapOf<NGFile, List<NGFile>>()

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (rootPageData.isEmpty()) {
            rootPageData = listDir("$mainUrl/").filter { it.isFolder && it.name != "ftp" }
        }

        val homePages = rootPageData.amap {
            val depth = if (it.name != "tv-series") 2 else 1
            val gdFiles = listDir(it, depth)
            cacheFiles[it] = gdFiles
            HomePageList(it.name, gdFiles.toSearchResponseList())
        }.filter { it.list.isNotEmpty() }

        return HomePageResponse(homePages)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        if (cacheFiles.isEmpty()) {
            rootPageData = listDir("$mainUrl/").filter { it.isFolder && it.name != "ftp" }
            rootPageData.amap {
                val depth = if (it.name != "tv-series") 2 else 1
                cacheFiles[it] = listDir(it, depth)
            }
        }
        val files = arrayListOf<NGFile>()
        cacheFiles.forEach { (_, fs) ->
            fs.forEach {
                if (it.name.contains(query, true)) {
                    files.add(it)
                }
            }
        }
        return files.toSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val file = tryParseJson<NGFile>(url) ?: return null
        val title = file.name

        var seasons: List<SeasonData>? = null

        val episodes = if (file.isFolder) {
            val items = listDir(file)
            val folders = items.filter { it.isFolder }
            val files =
                items.filter { !it.isFolder && it.name.contains("(?i)\\.(mkv|mp4)$".toRegex()) }
            seasons = folders.mapIndexed { i, f ->
                SeasonData(i + 1, "S\\d+".toRegex().find(f.name)?.value ?: f.name)
            }
            folders.amapIndexed { index, gdFile ->
                listDir(gdFile).mapNotNull {
                    if (!it.name.contains("(?i)\\.(mkv|mp4)$".toRegex())) return@mapNotNull null
                    newEpisode(it) {
                        name = "E\\d+".toRegex().find(it.name)?.value ?: it.name
                        season = index + 1
                    }
                }
            }.flatten().toMutableList().also {
                files.mapTo(it) { gdFile ->
                    newEpisode(gdFile) {
                        name = gdFile.name
                        season = seasons.size
                    }
                }
            }
        } else {
            arrayListOf(newEpisode(file) {
                name = title
            })
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            seasonNames = seasons
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val file = parseJson<NGFile>(data)
        val path = file.path
        callback(
            ExtractorLink(
                name,
                name,
                path,
                "",
                Qualities.Unknown.value,
            )
        )
        return true
    }

    private fun List<NGFile>.toSearchResponseList(): List<SearchResponse> {
        return filter { it.name.contains("(?i)\\.(mkv|mp4)$".toRegex()) || it.isFolder }.map {
            newAnimeSearchResponse(it.name, it.toJson())
        }
    }

    private suspend fun listDir(file: NGFile, depth: Int = 1): List<NGFile> {
        return listDir(file.path, depth)
    }

    private suspend fun listDir(file: String, depth: Int = 1): List<NGFile> {
        val doc = app.get(file, headers = headers).document
        val entries = doc.select("a").mapNotNull {
            if (it.text() == "../") return@mapNotNull null
            NGFile(
                name = URLDecoder.decode(it.attr("href"), "utf-8").removeSuffix("/"),
                isFolder = it.attr("href").endsWith("/"),
                parentFolder = file
            )
        }
        if (depth - 1 > 0) {
            val files = entries.filter { !it.isFolder }
            val entries1 = entries.filter { it.isFolder }.amap {
                listDir(it, depth - 1)
            }.flatten()
            return files + entries1
        }
        return entries
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

    private fun error(msg: String = "加载数据失败"): Nothing {
        throw ErrorLoadingException(msg)
    }

    data class NGFile(
        val name: String,
        val isFolder: Boolean,
        val parentFolder: String,
    ) {
        val path
            get() = "$parentFolder${
                name.replace("#", "%23").replace("?", "%3F")
            }" + if (isFolder) "/" else ""
    }

}
