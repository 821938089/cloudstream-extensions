package com.horis.cloudstreamplugins


class HaiwaikanProvider : UAPIProvider() {

    override var mainUrl = "http://api.haiwaikan.com/v1/vod"
    override var name = "海外看影视"
    override val playFromFilter = hashSetOf("haiwaikan")

}
