package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class Tellygossips(val source:String) : ExtractorApi() {
    override val mainUrl = "https://flow.tellygossips.net"
    override val name = "Tellygossips"
    override val requiresReferer = false
    private val referer = "http://tellygossips.net/"
    private val configRegex = "var config = ([\\s\\S]*?);".toRegex()
    private val headers = mapOf("Referer" to "$mainUrl/")

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url, referer = this.referer).document
        val configStr = doc.select("script")
            .map { it.data() }
            .firstOrNull { it.contains("var config = ") }
            ?.let { configRegex.find(it.trim())?.groupValues?.get(1) } ?: return
        val config = tryParseJson<Config>(configStr) ?: return
        for (link in config.sources) {
            callback(
                ExtractorLink(
                    source,
                    "$name ${link.label}",
                    link.file ?: link.src ?: continue,
                    "",
                    Qualities.Unknown.value,
                    true,
                    headers
                )
            )
        }
    }

    data class Config(
        val sources: List<VideoLink>,
    )

    data class VideoLink(
        val file: String?,
        val src: String?,
        val label: String,
        val type: String,
    )

}