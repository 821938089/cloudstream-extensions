package com.horis.cloudstreamplugins.apiextractors

import com.horis.cloudstreamplugins.Category
import com.horis.cloudstreamplugins.Vod
import com.horis.cloudstreamplugins.apis.VodAPI
import com.lagradost.cloudstream3.ErrorLoadingException
import org.jsoup.Jsoup
import org.jsoup.nodes.CDataNode
import org.jsoup.parser.Parser
import java.net.URLEncoder

class CollectXMLExtractor(val api: VodAPI) : VodAPIExtractor {
    var categoryCache: List<Category>? = null

    override suspend fun getCategory(limit: Int): List<Category> {
        categoryCache?.let { return it }
        val xml = api.list().text
        categoryCache = CategoryList(xml).list?.take(8)
        return categoryCache ?: throw ErrorLoadingException("获取分类数据失败")
    }

    override suspend fun getVodList(
        query: String?,
        page: Int,
        ids: String?,
        type: String?,
        pageSize: Int?
    ): List<Vod>? {
        val encodedQuery = query?.let { URLEncoder.encode(it, "utf-8") }
        val xml = api.list(encodedQuery, page, ids, type, pageSize).text
        return VodList(xml).list
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun getVodListDetail(
        query: String?,
        page: Int,
        ids: String?,
        type: String?,
        pageSize: Int?
    ): List<Vod>? {
        val encodedQuery = query?.let { URLEncoder.encode(it, "utf-8") }
        val xml = api.search(encodedQuery, page, ids, type, pageSize).text
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