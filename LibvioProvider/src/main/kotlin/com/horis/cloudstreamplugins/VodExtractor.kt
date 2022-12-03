package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class VodExtractor(html: String) {
    private val doc: Document = Jsoup.parse(html)
    var playerInfo: PlayerInfo? = null

    init {
        val playerInfoJSON = doc.select("script").firstOrNull {
            it.data().contains("var player_aaaa=")
        }?.data()?.substringAfter("var player_aaaa=")
        playerInfo = tryParseJson<PlayerInfo>(playerInfoJSON)
    }

    fun getPlayUrl(): String? {
        playerInfo ?: return null
        return when (playerInfo?.encrypt) {
            1 -> unescape(playerInfo?.url!!)
            2 -> unescape(base64Decode(playerInfo?.url!!))
            else /* 0 */ -> playerInfo?.url
        }
    }

}

data class PlayerInfo(
    @JsonProperty("encrypt") val encrypt: Int? = null,
    @JsonProperty("from") val from: String? = null,
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("nid") val nId: String? = null,
    @JsonProperty("link") val link: String? = null,
    @JsonProperty("link_next") val linkNext: String? = null,
    @JsonProperty("link_pre") val linkPre: String? = null,
    @JsonProperty("url") val url: String? = null
)