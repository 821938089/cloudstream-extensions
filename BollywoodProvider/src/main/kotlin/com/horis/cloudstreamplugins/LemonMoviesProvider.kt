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

class LemonMoviesProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "en"

    override var mainUrl = "http://103.134.58.242"
    override var name = "LemonMovies"

    override val hasMainPage = true

    private val headers = mapOf(
        "Referer" to "$mainUrl/",
    )

    private var rootPageData = emptyList<NGFile>()

    private val cacheFiles = hashMapOf<NGFile, List<NGFile>>()

    private val videoFileRegex = "(?i)\\.(mkv|mp4|ts|webm)$".toRegex()
    private val subtitleFileRegex = "(?i)\\.(srt|ssa|ass|vtt|ttml)$".toRegex()
    private val excludeFile =
        setOf("lost+found", "sofware1", "wget-log", "robots.txt", "wget-log.1")
    private val includeFile = setOf(
        "tv",
        "2020",
        "movies",
        "18-4-2023",
        "tvseries",
        "1950-2000",
        "2001-2010",
        "2011-2018",
        "2000-2014",
        "2015-2017",
        "2011-2020",
        "2015",
        "2016",
        "2017",
        "2018",
        "2019",
        "2020",
        "2021",
        "2022",
        "2023",
        "Hindi-dubbed",
        "animation",
        "bollywood",
        "hollywood",
        "imdb 250",
        "indian bangla",
        "korean",
        "t",
        "tamil",
        "Bangla",
        "Indian",
        "aneme",
        "english",
        "korean",
        "new",
        "t",
        "turkish",
        "tv10",
        "tv3",
        "tv4",
        "tv5",
        "tv6",
        "tv8",
        "tv9",
        "best",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (rootPageData.isEmpty()) {
            rootPageData = listDir("$mainUrl/Data/").filter {
                it.isFolder && !excludeFile.contains(it.name)
            }
        }

        val homePages = rootPageData.amap { file ->
            val gdFiles = listDir(file) { f, d ->
                if (d < 1) return@listDir true
                //if (includeFile.contains(f.parent?.name)) return@listDir true
                if (includeFile.contains(f.name)) return@listDir true
                false
            }.filter { !excludeFile.contains(it.name) }
            cacheFiles[file] = gdFiles
            HomePageList(file.name, gdFiles.toSearchResponseList())
        }.filter { it.list.isNotEmpty() }

        return HomePageResponse(homePages)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        if (cacheFiles.isEmpty()) {
            rootPageData = listDir("$mainUrl/").filter { it.isFolder && it.name != "ftp" }
            rootPageData.amap { file ->
                cacheFiles[file] = listDir(file) { f, d ->
                    if (d < 1) return@listDir true
                    //if (includeFile.contains(f.parent?.name)) return@listDir true
                    if (includeFile.contains(f.name)) return@listDir true
                    false
                }.filter { !excludeFile.contains(it.name) }
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
                items.filter { !it.isFolder && it.name.contains(videoFileRegex) }
            seasons = folders.mapIndexed { i, f ->
                SeasonData(i + 1, "S\\d+".toRegex().find(f.name)?.value ?: f.name)
            }
            folders.amapIndexed { index, gdFile ->
                listDir(gdFile).mapNotNull {
                    if (!it.name.contains(videoFileRegex)) return@mapNotNull null
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

        @Suppress("ObjectLiteralToLambda")
        episodes.sortWith(object : Comparator<Episode> {
            override fun compare(o1: Episode, o2: Episode): Int {
                return AlphanumComparator.compare(o1.name!!, o2.name!!)
            }
        })

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
        listDir(file.parentFolder).filter {
            it.name.contains(subtitleFileRegex)
        }.map {
            SubtitleFile(it.name, it.path)
        }.forEach(subtitleCallback)

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

    private suspend fun listDir(
        fileDir: String,
        depth: Int = 0,
        enter: (NGFile, Int) -> Boolean = { _, d -> d < 1 }
    ): List<NGFile> {
        return listDir(NGFile.fromDir(fileDir), depth, enter)
    }

    private suspend fun listDir(
        file: NGFile,
        depth: Int = 0,
        enter: (NGFile, Int) -> Boolean = { _, d -> d < 1 }
    ): List<NGFile> {
        val doc = app.get(file.path, headers = headers).document
        val entries = doc.select("a").mapNotNull {
            if (it.text() == "../") return@mapNotNull null
            NGFile(
                name = URLDecoder.decode(it.attr("href"), "utf-8").removeSuffix("/"),
                isFolder = it.attr("href").endsWith("/"),
                parentFolder = file.path,
                parent = file
            )
        }
        val files = entries.filter { !it.isFolder }
        val entries1 = entries.filter { it.isFolder && !enter.invoke(it, depth + 1) }
        val entries2 = entries.filter {
            it.isFolder && enter.invoke(it, depth + 1)
        }.amap {
            listDir(it, depth + 1, enter)
        }.flatten()
        return files + entries1 + entries2
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
        val parent: NGFile? = null
    ) {
        companion object {
            fun fromDir(url: String): NGFile {
                return NGFile(
                    url.removeSuffix("/").substringAfterLast("/"),
                    true,
                    url.removeSuffix("/").substringBeforeLast("/") + "/"
                )
            }
        }

        val path
            get() = ("$parentFolder${
                name.replace("#", "%23").replace("?", "%3F")
            }" + if (isFolder) "/" else "").replace(" ", "%20")


    }

}
