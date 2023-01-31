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

class PiousIndexProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "en"

    override var mainUrl = "https://drive.gamick.workers.dev"
    override var name = "PiousIndex"

    override val hasMainPage = true

    private val nextPageToken = ConcurrentHashMap<String, String>()

    private val headers = mapOf(
        "Referer" to "$mainUrl/",
        "Origin" to mainUrl,
    )

    private var rootPageData = emptyList<GDFile>()

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (rootPageData.isEmpty()) {
            rootPageData = listDir("$mainUrl/0:/")
        }

        val homePages = rootPageData.amap {
            val gdIndex = listDir(it.path, page, nextPageToken[it.name] ?: "")
            nextPageToken[it.name] = gdIndex.nextPageToken ?: ""
            HomePageList(it.name, gdIndex.data.files.toSearchResponseList())
        }

        return HomePageResponse(homePages)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val body = mapOf(
            "q" to query,
            "page_token" to "",
            "page_index" to "0"
        )
        val url = "$mainUrl/0:search"
        val gdIndex = tryParseJson<GDIndex>(
            resDecode(app.post(url, data = body, headers = headers).text)
        ) ?: throw ErrorLoadingException("parse index data fail (search)")
        return gdIndex.data.files.toSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val file = tryParseJson<GDFile>(url) ?: return null
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
        val file = parseJson<GDFile>(data)
        val path = if (file.parentFolder != null) {
            file.path
        } else {
            "$mainUrl/0:${id2Path(file.id)}"
        }
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

    private suspend fun id2Path(id: String): String {
        val body = mapOf("id" to id)
        return app.post("$mainUrl/0:id2path", data = body, headers = headers).text
    }

    private fun List<GDFile>.toSearchResponseList(): List<SearchResponse> {
        return filter { it.name.contains("(?i)\\.(mkv|mp4)$".toRegex()) || it.isFolder }.map {
            newAnimeSearchResponse(it.name, it.toJson())
        }
    }

    private suspend fun listDir(file: GDFile): List<GDFile> {
        val path = if (file.parentFolder != null) {
            file.path
        } else {
            "$mainUrl/0:${id2Path(file.id)}"
        }
        return listDir(path)
    }

    private suspend fun listDir(file: String, maxPage: Int = 999): List<GDFile> {
        var nextPageToken = ""
        var page = 0
        val files = arrayListOf<GDFile>()
        while (true) {
            if (page > maxPage) break
            val gdIndex = listDir(file, page, nextPageToken)
            files.addAll(gdIndex.data.files)
            nextPageToken = gdIndex.nextPageToken ?: break
            page++
        }
        return files
    }

    private suspend fun listDir(file: String, page: Int, nextPageToken: String?): GDIndex {
        val body = mapOf(
            "password" to "",
            "page_token" to (nextPageToken ?: ""),
            "page_index" to "$page"
        )
        val gdIndex = tryParseJson<GDIndex>(
            resDecode(app.post(file, data = body, headers = headers).text)
        ) ?: error("parse index data fail (listdir)")
        gdIndex.data.files.forEach { it.parentFolder = file }
        return gdIndex
    }

    private fun resDecode(str: String): String {
        val t = str.toList().reversed().joinToString("").substring(24)
        val t1 = t.substring(0, t.length - 20)
        return base64Decode(t1)
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
        val path
            get() = "$parentFolder${
                name.replace("#", "%23").replace("?", "%3F")
            }" + if (isFolder) "/" else ""
    }

}
