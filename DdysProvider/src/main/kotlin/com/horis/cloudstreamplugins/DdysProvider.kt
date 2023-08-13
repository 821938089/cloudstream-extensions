package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.nodes.Element

class DdysProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    override var mainUrl = "https://ddys.pro"
    override var name = "低端影视"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "$mainUrl/" to "首页",
        "$mainUrl/category/movie/" to "电影",
        "$mainUrl/category/drama/" to "剧集",
        "$mainUrl/category/anime/" to "动画",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data}page/$page/"
        }
        val doc = app.get(url, referer = "$mainUrl/").document
        val items = doc.select(".post-box-list article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val url = "$mainUrl/?s=$query&post_type=post"
        val doc = app.get(url, referer = "$mainUrl/").document
        return doc.select("#main article").mapNotNull {
            it.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val name = selectFirst(".post-box-title, .post-title")?.text() ?: return null
        val url = attr("data-href").ifEmpty {
            selectFirst("a")?.attr("href") ?: return null
        }
        return newTvSeriesSearchResponse(name, url) {
            posterUrl = selectFirst(".post-box-image")
                ?.attr("style")
                ?.substring("url(", ")")
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, referer = "$mainUrl/").document
        val name = doc.selectFirst(".post-title")?.text() ?: error("解析数据失败（标题）")
        val playListJson = doc.selectFirst(".wp-playlist-script")?.data() ?: error("获取播放列表失败")
        val playList = tryParseJson<PlayList>(playListJson) ?: error("解析播放列表数据失败")
        val seasons = doc.select(".post-page-numbers").mapIndexed { index, el ->
            SeasonData(index + 1, "第${el.text()}季")
        }
        val episodes = playList.tracks.map {
            newEpisode(it) {
                this.name = it.caption
                season = 1
            }
        }
        val episodesLeft = doc.select(".page-links a").amapIndexed { i, el->
            val doc1 = app.get(el.attr("href"), referer = "$mainUrl/").document
            val playListJson1 = doc1.selectFirst(".wp-playlist-script")?.data() ?: return@amapIndexed null
            val playList1 = tryParseJson<PlayList>(playListJson1) ?: return@amapIndexed null
            playList1.tracks.map {
                newEpisode(it) {
                    this.name = it.caption
                    season = i + 2
                }
            }
        }.filterNotNull().flatten()
        return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes + episodesLeft) {
            val abstracts = doc.selectFirst(".abstract")
                ?.textNodes()
                ?.map { it.text().trim() }
            year = abstracts?.find { it.startsWith("年份:") }
                ?.substringAfter("年份:")?.trim()?.toIntOrNull()
            plot = abstracts?.find { it.startsWith("简介:") }?.substringAfter("简介:")?.trim()
            posterUrl = doc.selectFirst(".doulist-item img")?.attr("src")
            posterHeaders = mapOf("Referer" to "$mainUrl/")
            tags = doc.select(".tags-links a").map { it.text() }
            seasonNames = seasons
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val track = parseJson<Track>(data)
        val api = when (track.srctype) {
            "1" -> "getvddr2"
            "2" -> "getvddr3"
            else -> "getvddr"
        }
        val linkCN = app.get(
            "$mainUrl/$api/video?id=${track.src1}&type=json",
            referer = "$mainUrl/"
        ).parsedSafe<PlayUrl>()
        linkCN?.let {
            callback(
                ExtractorLink(
                    name,
                    "国内节点",
                    it.url,
                    "",
                    Qualities.Unknown.value
                )
            )
        }
        if (track.src0.isNotBlank() && track.src2.isNotBlank()) {
            callback(
                ExtractorLink(
                    name,
                    "海外节点",
                    "https://v.ddys.pro${track.src0}?ddrkey=${track.src2}",
                    "$mainUrl/",
                    Qualities.Unknown.value
                )
            )
        }
        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request())
            }
        }
    }

    private fun error(msg: String = "加载数据失败"): Nothing {
        throw ErrorLoadingException(msg)
    }

    data class PlayList(
        val tracks: List<Track>
    )

    data class Track(
        val src0: String,
        val src1: String,
        val src2: String,
        val caption: String,
        val srctype: String
    )

    data class PlayUrl(
        val url: String
    )

}
