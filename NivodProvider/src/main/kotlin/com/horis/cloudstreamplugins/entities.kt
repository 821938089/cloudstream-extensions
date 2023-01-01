package com.horis.cloudstreamplugins

data class UserIdentity(
    val oid: String
)

data class NiVodHome(
    val list: List<Section>,
)

data class Section(
    val blockId: Int,
    val blockType: Int,
    val title: String,
    val rows: List<Row>
)

data class Row(
    val cells: List<Cell>,
)

data class Cell(
    val cellId: Int,
    val img: String,
    val show: Show,
    val title: String
)

data class Show(
    val actors: String,
    val episodesTxt: String,
    val regionName: String,
    val showIdCode: String,
    val showImg: String,
    val showTitle: String,
    val showTypeName: String
)

data class DetailResponse(
    val entity: Entity,
)

data class Entity(
    val showDesc: String,
    val showTypeName: String,
    val showIdCode: String,
    val showImg: String,
    val showTitle: String,
    val regionName: String,
    val actors: String,
    val postYear: String,
    val isEpisodes: Int,
    val plays: List<Play>,
    val playLangs: List<Lang>,
    val playSources: List<Source>,
)

data class Lang(
    val langId: String,
    val langName: String,
)

data class Source(
    val sourceId: String,
    val sourceName: String,
)

//data class PlayListResponse(
//    val list: List<Play>
//)

data class Play(
    val displayName: String,
    var showIdCode: String? = null,
    val playIdCode: String,
)

data class PlayInfoResponse(
    val entity: PlayInfo,
)

data class PlayInfo(
    val playType: Int,
    val playUrl: String,
)

data class SearchResp(
    val list: List<SearchEntity>,
)

data class SearchEntity(
    val showTitle: String,
    val showIdCode: String,
    val showImg: String,
)
