package com.horis.cloudstreamplugins

import android.annotation.SuppressLint
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.reflect.KClass

val JSONParser = object : ResponseParser {
    val mapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
        .configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true)
        .configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true)

    override fun <T : Any> parse(text: String, kClass: KClass<T>): T {
        return mapper.readValue(text, kClass.java)
    }

    override fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T? {
        return try {
            mapper.readValue(text, kClass.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun writeValueAsString(obj: Any): String {
        return mapper.writeValueAsString(obj)
    }
}

inline fun <reified T : Any> parseJson(text: String): T {
    return JSONParser.parse(text, T::class)
}

inline fun <reified T : Any> tryParseJson(text: String): T? {
    return try {
        return JSONParser.parseSafe(text, T::class)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

val app = Requests(responseParser = JSONParser).apply {
    defaultHeaders = mapOf("User-Agent" to USER_AGENT)
}

fun String.substring(left: String, right: String): String {
    return substringAfter(left).substringBefore(right)
}

/**
 * Escape解码
 *
 * @param content 被转义的内容
 * @return 解码后的字符串
 */
fun unescape(content: String): String {
    if (content.isBlank()) {
        return content
    }
    val tmp = StringBuilder(content.length)
    var lastPos = 0
    var pos: Int
    var ch: Char
    while (lastPos < content.length) {
        pos = content.indexOf("%", lastPos)
        if (pos == lastPos) {
            if (content[pos + 1] == 'u') {
                ch = content.substring(pos + 2, pos + 6).toInt(16).toChar()
                tmp.append(ch)
                lastPos = pos + 6
            } else {
                ch = content.substring(pos + 1, pos + 3).toInt(16).toChar()
                tmp.append(ch)
                lastPos = pos + 3
            }
        } else {
            lastPos = if (pos == -1) {
                tmp.append(content.substring(lastPos))
                content.length
            } else {
                tmp.append(content, lastPos, pos)
                pos
            }
        }
    }
    return tmp.toString()
}

@SuppressLint("GetInstance")
fun aesEncrypt(data: ByteArray, key: ByteArray): ByteArray {
    val aes = Cipher.getInstance("AES/ECB/PKCS5Padding")
    aes.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
    return aes.doFinal(data)
}
