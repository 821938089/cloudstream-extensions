package com.horis.cloudstreamplugins

import android.app.Activity
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

val app = Requests(responseParser = object : ResponseParser {
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
}).apply {
    defaultHeaders = mapOf("User-Agent" to BaseUAPIProvider.UserAgent)
}

var activityRef : WeakReference<Activity>? = null
val activity get() = activityRef?.get()

fun showToast(message: String, duration: Int? = null) {
    val activity = activity ?: return
    CommonActivity.showToast(activity, message, duration)
}