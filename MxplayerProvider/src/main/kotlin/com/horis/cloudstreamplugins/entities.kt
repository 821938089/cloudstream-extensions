package com.horis.cloudstreamplugins


data class Mxplayer(
    val config: Config,
    val entities: Map<String, Entity>,
    val platform: String,
    val homepage: HomePage?,
)

data class Config(
    val imageBaseUrl: String,
    val videoCdnBaseUrl: String
)

data class Entity(
    val contributors: List<Contributor>,
    val description: String?,
    val descriptor: List<String>?,
    val duration: Int,
    val genres: List<String>,
    val id: String,
    val imageInfo: List<Image>,
    val languages: List<String>,
    val partial: Boolean,
    val publishTime: String?,
    val rating: Int,
    val stream: Stream?,
    val trailer: List<Trailer>?,
    val title: String,
    val type: String,
    val videoCount: Int?,
    val tabs: List<Tab>?,
    val webUrl: String
) {
    val isTvShow get() = type == "tvshow"
}

data class Tab(
    val api: String,
    val containers: List<Container>?,
    val type: String,
    val title: String,
)

data class Container(
    val aroundApi: String,
    val id: String,
    val title: String,
    val type: String,
    val sequence: Int
)

data class Trailer(
    val stream: Stream
)

data class Stream(
    val hls: VideoLink?,
    val dash: VideoLink?,
    val mxplay: Mxplay?,
    val thirdParty: VideoLink?,
    val provider: String,
    val videoHash: String,
)

data class Mxplay(
    val hls: VideoLink?
)

data class VideoLink(
    val base: String?,
    val high: String?,
    val main: String?,
    val hlsUrl: String?,
    val dashUrl: String?,
)

data class Image(
    val density: String,
    val height: Int,
    val type: String,
    val url: String,
    val width: Int,
)

data class Contributor(
    val name: String,
    val type: String,
    val imageInfo: List<Image>?,
)

data class HomePage(
    val home: Home
)

data class Home(
    val next: String,
    val sections: List<Section>,
)

data class Section(
    val forceLoadImage: Boolean,
    val id: String,
    val items: List<String>,
    val name: String,
    val next: String,
    val style: String,
    val webUrl: String,
)

data class HomeDetail(
    val next: String?,
    val previous: String?,
    val sections: List<SectionDetail>,
)

data class SectionDetail(
    val id: String,
    val items: List<Entity>,
    val name: String,
    val next: String?,
    val style: String,
    val webUrl: String?,
)

data class EpisodesData(
    val id: String,
    val next: String?,
    val previous: String?,
    val items: List<Entity>,
)
