package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import java.net.URL

class Dailymotion : ExtractorApi() {
    override val mainUrl = "https://www.dailymotion.com"
    override val name = "Dailymotion"
    override val requiresReferer = false
    private val videoIdRegex = "^[kx][a-zA-Z0-9]+\$".toRegex()

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url).document
        val prefix = "window.__PLAYER_CONFIG__ = "
        val configStr = doc.select("script").map { it.data() }
            .firstOrNull { it.startsWith(prefix) }
            ?: return
        val config = tryParseJson<Config>(configStr.substringAfter(prefix)) ?: return
        val id = getVideoId(url) ?: return
        val dmV1st = config.dmInternalData.v1st
        val dmTs = config.dmInternalData.ts
        val metaDataUrl =
            "$mainUrl/player/metadata/video/$id?locale=en&dmV1st=$dmV1st&dmTs=$dmTs&is_native_app=0"
        val cookies = mapOf(
            "v1st" to dmV1st,
            "dmvk" to config.context.dmvk,
            "ts" to dmTs.toString()
        )
        val metaData = app.get(metaDataUrl, referer = url, cookies = cookies)
            .parsedSafe<MetaData>() ?: return
        metaData.qualities.forEach { (key, video) ->
            video.forEach {
                callback(
                    ExtractorLink(
                        name,
                        "$name $key",
                        it.url,
                        "",
                        Qualities.Unknown.value,
                        true
                    )
                )
            }
        }
    }

    private fun getVideoId(url: String): String? {
        val path = URL(url).path
        val id = path.substringAfter("video/")
        if (id.matches(videoIdRegex)) {
            return id
        }
        return null
    }

    data class Config(
        val context: Context,
        val dmInternalData: InternalData
    )

    data class InternalData(
        val ts: Int,
        val v1st: String
    )

    data class Context(
        @JsonProperty("access_token") val accessToken: String?,
        val dmvk: String,
    )

    data class MetaData(
        val qualities: Map<String, List<VideoLink>>
    )

    data class VideoLink(
        val type: String,
        val url: String
    )

}