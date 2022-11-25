package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.CDataNode
import org.jsoup.parser.Parser

abstract class UAPIXMLProvider : BaseUAPIProvider() {

    override suspend fun getCategory(): List<Category> {
        categoryCache?.let { return it }
        val xml = fetchApi("$mainUrl?ac=list").text
        categoryCache = CategoryList(xml).list?.take(8)
        return categoryCache ?: throw ErrorLoadingException("获取分类数据失败")
    }

    override suspend fun getVodList(url: String): List<Vod>? {
        val xml = fetchApi(url).text
        return VodList(xml).list
    }

    class VodList(xml: String) {
        var list: List<Vod>? = null

        init {
            val doc = Jsoup.parse(xml, Parser.xmlParser())
            list = doc.select("video").map {
                val id = it.selectFirst("id")?.text()?.toInt()
                val typeId = it.selectFirst("tid")?.text()?.toInt()
                val name = (it.selectFirst("name")?.childNode(0) as CDataNode).text()
                val pic = it.selectFirst("pic")?.text()
                val typeName = it.selectFirst("type")?.text()
                val lang = it.selectFirst("lang")?.text()
                val area = it.selectFirst("area")?.text()
                val year = it.selectFirst("year")?.text()
                val remarks = (it.selectFirst("note")?.childNode(0) as CDataNode).text()
                val actor = (it.selectFirst("actor")?.childNode(0) as CDataNode).text()
                val director = (it.selectFirst("director")?.childNode(0) as CDataNode).text()
                val playData = it.select("dl dd")
                val playFrom = playData.joinToString("$$$") {
                    it.attr("flag")
                }
                val playUrl = playData.joinToString("$$$") {
                    (it.childNode(0) as CDataNode).text()
                }
                val blurb = (it.selectFirst("des")?.childNode(0) as CDataNode).text()
                val time = it.selectFirst("last")?.text()
                Vod(
                    id = id,
                    typeId = typeId,
                    name = name,
                    pic = pic,
                    actor = actor,
                    director = director,
                    blurb = blurb,
                    remarks = remarks,
                    area = area,
                    lang = lang,
                    year = year,
                    time = time,
                    playFrom = playFrom,
                    playUrl = playUrl,
                    typeName = typeName
                )
            }
        }
    }

    class CategoryList(xml: String) {
        var list: List<Category>? = null

        init {
            val doc = Jsoup.parse(xml, Parser.xmlParser())
            list = doc.select("class ty").map {
                Category(it.attr("id").toInt(), it.text())
            }
        }
    }

}
