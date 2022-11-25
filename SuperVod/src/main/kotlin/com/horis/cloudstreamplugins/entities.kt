package com.horis.cloudstreamplugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.nicehttp.NiceResponse

data class Vod(
    @JsonProperty("vod_id") val id: Int? = null,
    @JsonProperty("type_id") val typeId: Int? = null,
    @JsonProperty("vod_name") val name: String? = null,
    @JsonProperty("vod_sub") val sub: String? = null,
    @JsonProperty("vod_pic") val pic: String? = null,
    @JsonProperty("vod_actor") val actor: String? = null,
    @JsonProperty("vod_director") val director: String? = null,
    @JsonProperty("vod_writer") val writer: String? = null,
    @JsonProperty("vod_blurb") val blurb: String? = null, // 简介
    @JsonProperty("vod_remarks") val remarks: String? = null,
    @JsonProperty("vod_area") val area: String? = null,
    @JsonProperty("vod_lang") val lang: String? = null,
    @JsonProperty("vod_year") val year: String? = null,
    @JsonProperty("vod_time") val time: String? = null,
    @JsonProperty("vod_content") val content: String? = null,
    @JsonProperty("vod_play_from") val playFrom: String? = null,
    @JsonProperty("vod_play_server") val playServer: String? = null,
    @JsonProperty("vod_play_note") val playNote: String? = null,
    @JsonProperty("vod_play_url") val playUrl: String? = null,
    @JsonProperty("type_name") val typeName: String? = null,
)

data class Category(
    @JsonProperty("type_id") val typeId: Int,
    @JsonProperty("type_name") val typeName: String
)

data class PlayData(
    val server: String,
    val url: String
)

data class PlayerInfo(
    @JsonProperty("encrypt") val encrypt: Int? = null,
    @JsonProperty("from") val from: String? = null,
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("link") val link: String? = null,
    @JsonProperty("link_next") val linkNext: String? = null,
    @JsonProperty("link_pre") val linkPre: String? = null,
    @JsonProperty("url") val url: String? = null
)

data class VodSource(
    val name: String,
    val apiUrl: String,
    val apiType: Int = 0,
    val responseType: Int = 0
)