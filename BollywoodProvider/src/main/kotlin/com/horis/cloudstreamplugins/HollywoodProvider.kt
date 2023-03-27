package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.mainPageOf

class HollywoodProvider : BollywoodProvider() {
    override var lang = "en"

    override var mainUrl = "https://hollywood.eu.org"
    override var name = "Hollywood"

    override val api = "https://simpleprogramenglishapi.zindex.eu.org"

    override val mainPage = mainPageOf(
        "$api/0:/Hollywood.English/" to "Hollywood English Movies",
        "$api/0:/Web.Series.English/" to "Web Series English",
    )
}
