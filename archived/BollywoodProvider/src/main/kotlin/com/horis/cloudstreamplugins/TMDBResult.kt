package com.horis.cloudstreamplugins

data class TMDBResult(
    val backdrop_path: String?,
    val genre_ids: List<Int>?,
    val media_type: String?,
    val seasons: List<Season>?,
    val id: Int,
    val name: String?,
    val title: String?,
    val release_date: String?,
    val overview: String,
    val popularity: Double,
    val poster_path: String?,
    val vote_average: Double,
    val vote_count: Int
)

data class Season(
    val id: Int,
    val name: String,
    val season_number: Int
)

data class SingleEpisode(
    val id: Int,
    val name: String,
    val overview: String,
    val still_path: String?,
    val episode_number: Int,
    val season_number: Int,
)

data class TMDBDetailEpisodes(
    val episodes: List<SingleEpisode>,
)
