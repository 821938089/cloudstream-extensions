package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import org.jsoup.nodes.Element

class BollyzoneProvider : DesicinemasProvider() {
    override val supportedTypes = setOf(
        TvType.TvSeries
    )
    override var lang = "en"

    override var mainUrl = "https://www.bollyzone.tv"
    override var name = "Bollyzone"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val doc = app.get(mainUrl).document

        val pages = doc.selectFirst(".MovieList")
            ?.toHomePageList("Latest Episode Updates")

        return HomePageResponse(arrayListOf(pages).filterNotNull())
    }

    private fun Element.toHomePageList(name: String): HomePageList {
        val items = select("li")
            .mapNotNull {
                it.toHomePageResult()
            }
        return HomePageList(name, items)
    }

    private fun Element.toHomePageResult(): SearchResponse? {
        val title = selectFirst("h2")?.text()?.trim() ?: return null
        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val img = selectFirst("img")
        val posterUrl = fixUrlNull(img?.attr("src"))

        return newAnimeSearchResponse(title, href) {
            this.posterUrl = posterUrl
        }
    }

}
