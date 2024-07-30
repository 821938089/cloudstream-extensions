package com.horis.cloudstreamplugins

data class TMDBData(
    val page: Int,
    val results: List<TMDBResult>,
    val total_pages: Int,
    val total_results: Int
)
