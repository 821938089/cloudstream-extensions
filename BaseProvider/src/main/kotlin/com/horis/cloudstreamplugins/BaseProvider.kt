package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.MainAPI

abstract class BaseProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Others
    )

    override var lang = "zh"

}