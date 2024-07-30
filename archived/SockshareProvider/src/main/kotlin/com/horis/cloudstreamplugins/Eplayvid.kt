package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class Eplayvid : ExtractorApi() {
    override val mainUrl = "https://eplayvid.net"
    override val name = "Eplayvid"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url, referer = "$mainUrl/").document
        doc.select("video source").map {
            callback(
                ExtractorLink(
                    name,
                    name,
                    it.attr("src"),
                    "$mainUrl/",
                    Qualities.Unknown.value
                )
            )
        }
    }

}