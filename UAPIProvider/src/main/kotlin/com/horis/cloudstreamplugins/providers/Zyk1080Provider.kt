package com.horis.cloudstreamplugins


class Zyk1080Provider : UAPIProvider() {

    override var mainUrl = "https://api.1080zyku.com/inc/apijson.php/provide/vod/"
    override var name = "优质影视"
    override val playFromFilter = hashSetOf("1080zyk")

}
