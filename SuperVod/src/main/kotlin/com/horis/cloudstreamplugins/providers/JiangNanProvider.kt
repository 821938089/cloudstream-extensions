package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.FormBody
import okio.ByteString.Companion.encode
import java.net.URLDecoder


class JiangNanProvider : BaseVodProvider() {

    override var mainUrl = "https://api.yjiexi.com/provide/xml.html"
    override var name = "江南影视"
    override val apiExtractor by lazy {
        makeApiExtractor(mainUrl, responseType = 1)
    }
    override val filterM3U8Url = false
    //override val playFromFilter = hashSetOf("youku", "qiyi")

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val playData = AppUtils.parseJson<PlayData>(data)
        val link = "https://player.yjiexi.com/player?url=${playData.url}"
        val res = app.get(link)
        val vid: String = "vid: '(.*?)'".toRegex().find(res.text)!!.groupValues[1]
        val url: String = "(?<!next)url: '(.*?)'".toRegex().find(res.text)!!.groupValues[1]
        val token: String = "token: '(.*?)'".toRegex().find(res.text)!!.groupValues[1]

        val body = mutableMapOf(
            "vid" to vid,
            "url" to url,
            "token" to token
        )
        body["sign"] = createSign(body)

        val headers = mapOf(
            "Referer" to link,
            "X-Requested-With" to "XMLHttpRequest",
        )

        val formBody = FormBody.Builder().run {
            body.forEach {
                add(it.key, it.value)
            }
            build()
        }

        val result = app.post(
            "https://player.yjiexi.com/api",
            requestBody = formBody, headers = headers
        ).parsed<JNResult>()

        callback(
            ExtractorLink(
                name,
                playData.server,
                result.playurl,
                link,
                Qualities.Unknown.value,
                result.type == "m3u8"
            )
        )

        return true
    }

    private fun createSign(
        bodyMap: MutableMap<String, String> = mutableMapOf(),
        key: String = "JNPLAYER789",
        t: Long = 0
    ): String {
        var timestamp = (System.currentTimeMillis() / 1000).toString()
        if (t > 0) timestamp = t.toString()
        bodyMap["t"] = timestamp
        bodyMap["tkey"] = "$key$timestamp".encode().md5().hex()
        val body = bodyMap.entries.sortedBy { it.key }
            .fold("") { last, (key, value) ->
                if (key.isEmpty() || value.isEmpty() || key == "sign") last else
                    "$last$key=$value&"
            }.removeSuffix("&")
        bodyMap.remove("tkey")
        return "$body$key".encode().md5().hex().uppercase()
    }

    private fun decodeBackSlashX(s: String) : String {
        return URLDecoder.decode(s.replace("\\x", "%"), "utf-8")
    }

    data class JNResult(
        val playurl: String,
        val type: String
    )
}
