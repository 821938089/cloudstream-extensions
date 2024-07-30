package com.horis.cloudstreamplugins

import android.annotation.SuppressLint
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.mapper
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.reflect.KClass

val JSONParser = object : ResponseParser {
    val mapper: ObjectMapper = jacksonObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
    ).configure(
        JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true
    )

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

val app = Requests(responseParser = JSONParser).apply {
    defaultHeaders = mapOf("User-Agent" to USER_AGENT)
}

fun String.substring(left: String, right: String): String {
    return substringAfter(left).substringBefore(right)
}

@SuppressLint("GetInstance")
fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
    val aes = Cipher.getInstance("AES/CBC/PKCS7Padding")
    aes.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
    return aes.doFinal(data)
}

fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
    val aes = Cipher.getInstance("AES/CBC/PKCS5Padding")
    aes.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
    return aes.doFinal(data)
}

inline fun <reified T> NiceResponse.parse(): T {
    return mapper.readValue(text, object : TypeReference<T>() {})
}
