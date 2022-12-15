package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.nicehttp.NiceResponse
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class MxplayerProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama,
    )
    override var lang = "en"

    override var mainUrl = "https://www.mxplayer.in"
    override var name = "Mxplayer"

    override val hasMainPage = true

    private var userID: String? = null

    private var mxplayer: Mxplayer? = null

    private val webApi = "https://api.mxplayer.in/v1/web"

    private val endParam
        get() = "&device-density=2&userid=$userID&platform=com.mxplay.desktop" +
                "&content-languages=hi,en&kids-mode-enabled=false"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val res = app.get(mainUrl)
        userID = res.okhttpResponse.headers.getCookies()["UserID"]
            ?: throw ErrorLoadingException("load fail, geo blocked")

        mxplayer = getMxplayer(res)

        val pages1 = mxplayer!!.homepage!!.home.sections.map {
            it.toHomePageList(mxplayer!!)
        }
        val home2 = app.get(
            "$webApi/home/tab/7694f56f59238654b3a6303885f9166f" +
                    "?next=f9eddeb5c3d23902a46ee44de711f62e$endParam",
            referer = "$mainUrl/"
        ).parsedSafe<HomeDetail>() ?: throw ErrorLoadingException("load data2 fail")

        val pages2 = home2.sections.map {
            it.toHomePageList()
        }

        return HomePageResponse(pages1 + pages2)
    }

    private fun Section.toHomePageList(mxplayer: Mxplayer): HomePageList {
        val items = items.mapNotNull { vid ->
            val video = mxplayer.entities[vid] ?: return@mapNotNull null
            video.toSearchResult()
        }
        return HomePageList(name, items)
    }

    private fun Entity.toSearchResult(): SearchResponse {
        return newAnimeSearchResponse(title, this.toJson()) {
            posterUrl = imageInfo.getUrl()
        }
    }

    private fun SectionDetail.toHomePageList(): HomePageList {
        val items = items.map {
            it.toSearchResult()
        }
        return HomePageList(name, items)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        if (userID == null) {
            val res = app.get(mainUrl)
            userID = res.okhttpResponse.headers.getCookies()["UserID"]
        }

        val data = app.post(
            "$webApi/search/resultv2?query=$query$endParam",
            referer = "$mainUrl/",
            requestBody = "{}".toRequestBody("application/json".toMediaType())
        ).parsed<HomeDetail>()

        return data.toSearchResponseList()
    }

    private fun HomeDetail.toSearchResponseList(): List<SearchResponse> {
        return sections.map { section ->
            section.items.map { it.toSearchResult() }
        }.flatten()
    }


    override suspend fun load(url: String): LoadResponse? {
        val video = tryParseJson<Entity>(url) ?: return null
        val title = video.title
        val seasons = arrayListOf<SeasonData>()

        var video1: Entity? = null

        val episodes = if (video.isTvShow) {
            val webUrl = fixUrl(video.webUrl)
            val mxplayer = getMxplayer(webUrl)
            video1 = mxplayer.entities.firstNotNullOf { (_, v) -> v }
            val tab = video1.tabs?.firstOrNull { it.type == "tvshowepisodes" }
                ?: throw ErrorLoadingException("tvshowepisodes tab not found")
            val containers = tab.containers ?: throw ErrorLoadingException("containers not found")
            containers.amap {
                seasons.add(SeasonData(it.sequence, it.title))
                val episodes = arrayListOf<Episode>()
                var episodeUrl = "$webApi/${it.aroundApi}$endParam"
                while (true) {
                    val data = app.get(episodeUrl, referer = "$mainUrl/").parsedSafe<EpisodesData>()
                        ?: throw ErrorLoadingException("load episodes data fail")
                    data.items.mapTo(episodes) { episode ->
                        newEpisode(episode.stream) {
                            name = episode.title
                            posterUrl = episode.imageInfo.getUrl("landscape")
                            season = it.sequence
                        }
                    }
                    data.next ?: break
                    episodeUrl =
                        "$webApi/detail/tab/tvshowepisodes?type=season&${data.next}&id=${data.id}&sortOrder=0$endParam"
                }
                episodes
            }.flatten()
        } else {
            arrayListOf(newEpisode(video.stream!!) {
                name = title
                description = video.description
            })
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            posterUrl = video.imageInfo.getUrl()
            backgroundPosterUrl = video.imageInfo.getUrl("landscape")
            plot = video1?.description ?: video.description
            tags = arrayListOf<String>().apply {
                (video1?.descriptor ?: video.descriptor)?.let { addAll(it) }
                addAll(video1?.genres ?: video.genres)
                addAll(video1?.languages ?: video.languages)
            }
            seasonNames = seasons
        }

    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val stream = parseJson<Stream>(data)
        loadVideoLink(stream.mxplay?.hls, callback)
        loadVideoLink(stream.thirdParty, callback)
        return true
    }

    private fun loadVideoLink(link: VideoLink?, callback: (ExtractorLink) -> Unit) {
        link ?: return
        link.base?.let {
            addVideoLink(callback, it, "base")
        }
        link.main?.let {
            addVideoLink(callback, it, "main")
        }
        link.high?.let {
            addVideoLink(callback, it, "high")
        }
        link.hlsUrl?.let {
            addVideoLink(callback, it, "thirdParty")
        }
    }

    private fun addVideoLink(callback: (ExtractorLink) -> Unit, url: String, name: String) {
        val newUrl = if (!url.startsWith("http")) {
            mxplayer!!.config.videoCdnBaseUrl + url
        } else {
            url
        }
        callback(
            ExtractorLink(
                this.name,
                name,
                newUrl,
                "$mainUrl/",
                Qualities.Unknown.value,
                true
            )
        )
    }

    private fun List<Image>.getUrl(type: String = "portrait_large"): String? {
        return firstOrNull { it.type == type }?.let {
            mxplayer!!.config.imageBaseUrl + "/" + it.url
        }
    }

    private suspend fun getMxplayer(url: String): Mxplayer {
        val res = app.get(url, referer = "$mainUrl/")
        return getMxplayer(res)
    }

    private fun getMxplayer(res: NiceResponse): Mxplayer {
        val data = res.document.select("script")
            .firstOrNull { it.data().startsWith("window.state = ") }
            ?.data() ?: throw ErrorLoadingException("load data fail (main page)")
        return parseJson<Mxplayer>(data.substringAfter("window.state = "))
            ?: throw ErrorLoadingException("parse mxplayer data fail")
    }

    private fun Headers.getCookies(cookieKey: String = "set-cookie"): Map<String, String> {
        // Get a list of cookie strings
        // set-cookie: name=value; -----> name=value
        val cookieList =
            this.filter { it.first.equals(cookieKey, ignoreCase = true) }.map {
                it.second.split(";").firstOrNull()
            }.filterNotNull()

        // [name=value, name2=value2] -----> mapOf(name to value, name2 to value2)
        return cookieList.associate {
            val split = it.split("=", limit = 2)
            (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
        }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
    }

}
