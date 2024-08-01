package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.network.CloudflareKiller
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup

class CfBypass(private val cloudflareKiller: CloudflareKiller): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val doc = Jsoup.parse(response.peekBody(1024 * 1024).string())
        if (doc.select("title").text() == "Just a moment...") {
            return cloudflareKiller.intercept(chain)
        }
        return response
    }
}
