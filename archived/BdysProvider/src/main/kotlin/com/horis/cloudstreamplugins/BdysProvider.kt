package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.toByteString
import org.jsoup.nodes.Element

class BdysProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )
    override var lang = "zh"

    override var mainUrl = "https://www.bdys01.com"
    override var name = "哔嘀影视"

    override val hasMainPage = true
    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val cfBypass by lazy { CfBypass(cloudflareKiller) }

    override val mainPage by lazy {
        mainPageOf(*mainPages.toList().map { (k, v) -> v to k }.toTypedArray())
    }

    private val mainPages = mapOf(
        "最新电影" to "$mainUrl/s/all%s?type=0",
        "最新剧集" to "$mainUrl/s/all%s?type=1",
        "国产剧集" to "$mainUrl/s/all%s?type=1&area=%E4%B8%AD%E5%9B%BD%E5%A4%A7%E9%99%86",
        "港台剧集" to "$mainUrl/s/gangtaiju%s",
        "动画" to "$mainUrl/s/donghua%s?type=1",
        "欧美剧集" to "$mainUrl/s/meiju%s",
        "日韩剧集" to "$mainUrl/s/hanju%s",
    )

    private var cookies: Map<String, String> = mapOf()

    override var sequentialMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (page == 1 && request.name == "最新电影") {
            val res = app.get(mainUrl, interceptor = cfBypass)
            cookies = res.cookies
            val doc = res.document
            val categories = doc.select(".row-cards center ~ div").chunked(2)
            val homePages = categories.mapNotNull { (header, container) ->
                val title = header.selectFirst("h2")?.text() ?: return@mapNotNull null
                val items = container.select(".card").mapNotNull { it.toSearchResult() }
                HomePageList(title, items)
            }
            return HomePageResponse(homePages, true)
        } else if (page == 1) {
            return null
        }

        val page1 = if (page - 1 == 1) "" else "/${page - 1}"
        val url = (mainPages[request.name] ?: return null).replace("%s", page1)
        val doc = app.get(
            url,
            referer = "$mainUrl/", interceptor = cfBypass, cookies = cookies
        ).document
        val items = doc.select(".row-cards .card").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        if (query.startsWith("http")) {
            return listOf(load(query)?.toSearchResponse() ?: return null)
        }
        return null
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val name = selectFirst("h3")?.text() ?: return null
        val url = selectFirst("a")?.attr("href") ?: return null
        return newTvSeriesSearchResponse(name, url) {
            val img = selectFirst("img")
            posterUrl = img?.attr("data-src")?.ifBlank {
                img.attr("src")
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, referer = "$mainUrl/", cookies = cookies).document
        val name = doc.selectFirst("h2")?.text() ?: error("解析数据失败（标题）")
        val episodes = doc.select("#play-list a").map {
            val url1 = fixUrl(it.attr("href"))
            newEpisode(url1) {
                this.name = it.text()
            }
        }
        return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
            posterUrl = doc.selectFirst(".card-body img")?.attr("src")
            tags = doc.select(".page-header .badge").map { it.text() }
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val html = app.get(
            data,
            referer = "$mainUrl/", interceptor = cfBypass, cookies = cookies
        ).text
        val pid = html.substring("var pid = ", ";")
        val time = System.currentTimeMillis()
        val s1 = "$pid-$time"
        val key = s1.encode().md5().hex().substring(0, 16)
        val sign = aesEncrypt(s1.toByteArray(), key.toByteArray()).toByteString().hex().uppercase()
        val lines = app.get(
            "$mainUrl/lines?t=$time&sg=$sign&pid=$pid",
            referer = "$mainUrl/",
            interceptor = cfBypass,
            cookies = cookies
        ).parsed<LinesData>()

        if (lines.code != 0) {
            return false
        }

        val links = arrayListOf<String>()

        lines.data?.m3u8?.let {
            links.add(it.replace("https://www.bde4.cc", mainUrl))
        }

        lines.data?.m3u8_2?.let {
            links.addAll(it.split(",").map { l ->
                l.replace("https://www.bde4.cc", mainUrl)
            })
        }

        val body = FormBody.Builder()
            .addEncoded("t", time.toString())
            .addEncoded("sg", sign)
            .addEncoded("verifyCode", "888")
            .build()

        lines.data?.tos?.let {
            app.post("$mainUrl/god/$pid?type=1", requestBody = body, cookies = cookies)
                .parsedSafe<PlayUrl>()?.let { playUrl ->
                    links.add(playUrl.url)
                }
        }

        links.shuffled()
            .mapIndexed { i, it ->
                it.toLink("线路${i + 1}")
            }
            .forEach(callback)

        return true
    }

    private fun String.toLink(name: String = this@BdysProvider.name): ExtractorLink {
        return ExtractorLink(
            this@BdysProvider.name,
            name,
            this,
            "",
            Qualities.Unknown.value,
            this.contains(".m3u8"),
        )
    }

    private fun LoadResponse.toSearchResponse(): SearchResponse {
        return newTvSeriesSearchResponse(name, url) {
            posterUrl = this@toSearchResponse.posterUrl
        }
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request().newBuilder().removeHeader("referer").build()
                val response = chain.proceed(request)
                val priorUrl = response.priorResponse?.request?.url?.toString() ?: return response
                if (priorUrl.contains(".m3u8")) {
                    val fullBytes = response.body.bytes()
                    val compressed = fullBytes.sliceArray(3354 until fullBytes.size)
                    val manifest = GzipUtil.decompress(compressed)
                    if (manifest.isEmpty()) {
                        return response
                    }
                    val tsRegex = ".*?\\.ts".toRegex()
                    val manifestFix = manifest.replace(tsRegex, "https://vod.bdys.me/$0")
                    val contentType = "application/vnd.apple.mpegurl".toMediaType()
                    val body = manifestFix.toResponseBody(contentType)
                    return response.newBuilder().body(body).build()
                }
                return response
            }
        }
    }

    private fun error(msg: String = "加载数据失败"): Nothing {
        throw ErrorLoadingException(msg)
    }


    data class LinesData(
        val code: Int,
        val data: Lines?,
        val msg: String
    )

    data class Lines(
        val m3u8: String?,
        val m3u8_2: String?,
        val ptoken: String?,
        val tos: String?,
        val url3: String?,
    )

    data class PlayUrl(
        val url: String
    )

}
