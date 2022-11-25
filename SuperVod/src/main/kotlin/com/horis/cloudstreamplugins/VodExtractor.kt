package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class VodExtractor(html: String) {
    private val doc: Document = Jsoup.parse(html)

    fun getPlayUrl(): String? {
        val playerInfoJSON = doc.select("script").firstOrNull {
            it.data().contains("var player_aaaa=")
        }?.data()?.substringAfter("var player_aaaa=")
        val playerInfo = tryParseJson<PlayerInfo>(playerInfoJSON) ?: return null
        return when (playerInfo.encrypt) {
            1 -> unescape(playerInfo.url!!)
            2 -> unescape(base64Decode(playerInfo.url!!))
            else /* 0 */ -> playerInfo.url
        }
    }
}