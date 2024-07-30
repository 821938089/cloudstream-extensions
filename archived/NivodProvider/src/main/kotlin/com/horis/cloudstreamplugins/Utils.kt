package com.horis.cloudstreamplugins

import android.annotation.SuppressLint
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.ByteString.Companion.decodeHex
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

@SuppressLint("GetInstance")
fun aesEncrypt(data: ByteArray, key: ByteArray): ByteArray {
    val aes = Cipher.getInstance("AES/ECB/PKCS5Padding")
    aes.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
    return aes.doFinal(data)
}

@SuppressLint("GetInstance")
fun desDecrypt(data: ByteArray, key: ByteArray): ByteArray {
    val aes = Cipher.getInstance("DES/ECB/PKCS7Padding")
    aes.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "DES"))
    return aes.doFinal(data)
}

class DecryptInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val hexBody = response.peekBody(Long.MAX_VALUE).string()
        val contentType = response.body.contentType()
        return try {
            val encryptData = hexBody.decodeHex().toByteArray()
            val data = String(desDecrypt(encryptData, "diao.com".toByteArray()))
            val body = data.toResponseBody(contentType)
            response.newBuilder().body(body).build()
        } catch (e: Exception) {
            e.printStackTrace()
            response
        }
    }

}
