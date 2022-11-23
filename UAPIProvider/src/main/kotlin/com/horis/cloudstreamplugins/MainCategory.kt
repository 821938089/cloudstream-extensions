package com.horis.cloudstreamplugins

enum class MainCategory(value: Int?) {
    Movies(1),
    TVSeries(2),
    Anime(4),
    Variety(3), // 综艺
    EthicalFilms(5), // 伦理片
    Documentary(17), // 纪录片
    Information(36) // 资讯
}

val MainCategory.cnName get() = when (this) {
    MainCategory.Movies -> "电影"
    MainCategory.TVSeries -> "电视剧"
    MainCategory.Variety -> "综艺"
    MainCategory.Anime -> "动漫"
    MainCategory.EthicalFilms -> "伦理片"
    MainCategory.Documentary -> "纪录片"
    MainCategory.Information -> "资讯"
}
